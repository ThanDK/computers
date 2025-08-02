package in.project.computers.service.dashboard;

import in.project.computers.DTO.dashboard.DashboardResponse;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import in.project.computers.entity.order.LineItemType; // <-- IMPORT THIS
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.OrderStatus;
import in.project.computers.entity.order.PaymentStatus;

import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.repository.generalReposiroty.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Override
    public DashboardResponse getDashboardData(Instant startDate, Instant endDate) {
        log.info("Fetching dashboard data from {} to {}", startDate, endDate);

        List<Order> ordersInRange = orderRepository.findByCreatedAtBetween(startDate, endDate);

        long durationInDays = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        Instant previousPeriodStartDate = startDate.minus(durationInDays, ChronoUnit.DAYS);
        List<Order> prevOrdersInRange = orderRepository.findByCreatedAtBetween(previousPeriodStartDate, startDate);


        List<Order> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc();
        List<DashboardResponse.LowStockProduct> lowStockProducts = getLowStockProducts();

        DashboardResponse.Stats stats = calculateStats(ordersInRange, prevOrdersInRange, lowStockProducts);

        List<DashboardResponse.ChartData> revenueData = processRevenueChartData(ordersInRange);
        List<DashboardResponse.ChartData> topSellingData = processTopSellingChartData(ordersInRange);
        List<DashboardResponse.RecentOrder> recentOrdersDto = formatRecentOrders(recentOrders);

        return DashboardResponse.builder()
                .stats(stats)
                .revenueChartData(revenueData)
                .topSellingData(topSellingData)
                .recentOrders(recentOrdersDto)
                .lowStockProducts(lowStockProducts)
                .build();
    }

    @Override
    public List<DashboardResponse.RecentOrder> getOrdersForExport(Instant startDate, Instant endDate) {
        log.info("Fetching all orders from {} to {} for export", startDate, endDate);
        List<Order> ordersToExport = orderRepository.findByCreatedAtBetween(startDate, endDate);
        return formatRecentOrders(ordersToExport);
    }



    private List<DashboardResponse.LowStockProduct> getLowStockProducts() {
        List<Inventory> lowStockInventories = inventoryRepository.findAll()
                .stream()
                .filter(inv -> inv.getQuantity() < LOW_STOCK_THRESHOLD && inv.getQuantity() > 0)
                .toList();

        if (lowStockInventories.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> componentIds = lowStockInventories.stream()
                .map(Inventory::getComponentId)
                .collect(Collectors.toList());

        Map<String, Component> componentMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Function.identity()));

        return lowStockInventories.stream()
                .map(inventory -> {
                    Component component = componentMap.get(inventory.getComponentId());
                    if (component == null) {
                        return null;
                    }
                    return DashboardResponse.LowStockProduct.builder()
                            .id(component.getId())
                            .name(component.getName())
                            .mpn(component.getMpn())
                            .stock(inventory.getQuantity())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(DashboardResponse.LowStockProduct::getStock))
                .collect(Collectors.toList());
    }

    private List<DashboardResponse.RecentOrder> formatRecentOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> {
                    String customerName;
                    if (order.getEmail() != null && !order.getEmail().isBlank()) {
                        customerName = order.getEmail();
                    } else if (order.getUserId() != null && !order.getUserId().isBlank()) {
                        customerName = "User ID: " + order.getUserId();
                    } else {
                        customerName = "Guest Customer";
                    }
                    return DashboardResponse.RecentOrder.builder()
                            .id(order.getId())
                            .customerName(customerName)
                            .orderStatus(order.getOrderStatus().name())
                            .totalAmount(order.getTotalAmount())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private DashboardResponse.Stats calculateStats(List<Order> currentOrders, List<Order> previousOrders, List<DashboardResponse.LowStockProduct> lowStockProducts) {
        double totalRevenue = currentOrders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalAmount().doubleValue()).sum();
        long totalSales = currentOrders.size();

        double prevTotalRevenue = previousOrders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.COMPLETED)
                .mapToDouble(o -> o.getTotalAmount().doubleValue()).sum();
        long prevTotalSales = previousOrders.size();

        double revenueChange = (prevTotalRevenue > 0) ? ((totalRevenue - prevTotalRevenue) / prevTotalRevenue) * 100 : (totalRevenue > 0 ? 100 : 0);
        double salesChange = (prevTotalSales > 0) ? (((double)totalSales - prevTotalSales) / prevTotalSales) * 100 : (totalSales > 0 ? 100 : 0);

        List<OrderStatus> pendingStatuses = List.of(OrderStatus.PROCESSING, OrderStatus.REFUND_REQUESTED);

        long pendingOrdersCount = currentOrders.stream()
                .filter(o -> pendingStatuses.contains(o.getOrderStatus()))
                .count();

        long lowStockAlerts = lowStockProducts.size();

        return DashboardResponse.Stats.builder()
                .totalRevenue(totalRevenue)
                .revenueChange(revenueChange)
                .totalSales(totalSales)
                .salesChange(salesChange)
                .pendingOrders(pendingOrdersCount)
                .alerts(lowStockAlerts)
                .products(componentRepository.count())
                .build();
    }

    private List<DashboardResponse.ChartData> processRevenueChartData(List<Order> orders) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
        Map<String, BigDecimal> revenueByDate = orders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        order -> formatter.format(order.getCreatedAt()),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)
                ));
        return revenueByDate.entrySet().stream()
                .map(entry -> DashboardResponse.ChartData.builder()
                        .name(entry.getKey())
                        .value(entry.getValue().doubleValue())
                        .build())
                .sorted(Comparator.comparing(DashboardResponse.ChartData::getName))
                .collect(Collectors.toList());
    }


    private List<DashboardResponse.ChartData> processTopSellingChartData(List<Order> orders) {
        Map<String, Integer> componentQuantities = new HashMap<>();


        orders.stream()
                .filter(order -> order.getPaymentStatus() == PaymentStatus.COMPLETED)
                .flatMap(order -> order.getLineItems().stream())
                .forEach(item -> {

                    if (item.getItemType() == LineItemType.COMPONENT) {
                        componentQuantities.merge(item.getName(), item.getQuantity(), Integer::sum);
                    } else if (item.getItemType() == LineItemType.BUILD && item.getContainedItems() != null) {

                        item.getContainedItems().forEach(part -> {

                            int totalPartQuantity = part.getQuantity() * item.getQuantity();
                            componentQuantities.merge(part.getName(), totalPartQuantity, Integer::sum);
                        });
                    }
                });


        return componentQuantities.entrySet().stream()
                .map(entry -> DashboardResponse.ChartData.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .sorted(Comparator.comparingDouble(DashboardResponse.ChartData::getValue).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }
}