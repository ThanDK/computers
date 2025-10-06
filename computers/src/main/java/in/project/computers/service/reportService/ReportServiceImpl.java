package in.project.computers.service.reportService;

import in.project.computers.DTO.report.*;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import in.project.computers.entity.order.*;
import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.generalReposiroty.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;
    private final ComponentRepository componentRepository;
    private final MongoTemplate mongoTemplate;

    private static final int LOW_STOCK_THRESHOLD = 10;

    private static final Map<String, String> FIELD_PATH_MAP = Map.of(
            "paymentMethod", "paymentDetails.paymentMethod"
    );

    private static final Map<String, Class<?>> FIELD_TYPE_MAP = Map.of(
            "totalAmount", BigDecimal.class,
            "createdAt", Instant.class
    );

    @Override
    public OrderSearchApiResponse searchOrders(OrderSearchRequest request) {
        log.info("Performing dynamic order search with request: {}", request);

        Criteria criteria = new Criteria();
        if (request.getStartDate() != null && request.getEndDate() != null) {
            criteria.and("createdAt").gte(request.getStartDate()).lt(request.getEndDate());
        }

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            List<Criteria> filterCriteria = new ArrayList<>();
            for (FilterCriterion filter : request.getFilters()) {
                if (filter.getValues() == null || filter.getValues().isEmpty() || filter.getValues().getFirst().isBlank()) {
                    continue;
                }
                filterCriteria.add(createCriteriaFromFilter(filter));
            }

            if (!filterCriteria.isEmpty()) {
                String logic = (request.getLogic() != null) ? request.getLogic().toUpperCase() : "AND";
                if ("OR".equals(logic)) {
                    criteria.orOperator(filterCriteria);
                } else {
                    criteria.andOperator(filterCriteria);
                }
            }
        }

        Query query = new Query(criteria);
        List<Order> orders = mongoTemplate.find(query, Order.class);

        if (orders.isEmpty()) {
            return OrderSearchApiResponse.builder()
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .orders(Collections.emptyList())
                    .build();
        }

        Map<String, UserEntity> userMap = getUsersForOrders(orders);

        List<OrderReportDto> orderDtos = orders.stream().map(order -> {
            UserEntity user = userMap.get(order.getUserId());
            String customerName = (user != null) ? user.getName() : "N/A";
            String customerEmail = (user != null) ? user.getEmail() : order.getEmail();

            return OrderReportDto.builder()
                    .orderId(order.getId())
                    .orderDate(order.getCreatedAt())
                    .lastUpdateDate(order.getUpdatedAt())
                    .customerName(customerName)
                    .customerEmail(customerEmail)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentDetails() != null ? order.getPaymentDetails().getPaymentMethod().name() : "N/A")
                    .orderStatus(order.getOrderStatus().name())
                    .paymentStatus(order.getPaymentStatus().name())
                    .build();
        }).collect(Collectors.toList());

        return OrderSearchApiResponse.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .orders(orderDtos)
                .build();
    }

    private Criteria createCriteriaFromFilter(FilterCriterion filter) {
        String apiField = filter.getField();
        String dbPath = FIELD_PATH_MAP.getOrDefault(apiField, apiField);
        List<String> values = filter.getValues();
        FilterOperator operator = filter.getOperator();
        Object parsedValue = parseValueForField(apiField, values.getFirst());

        return switch (operator) {
            case IN -> Criteria.where(dbPath).in(values);
            case NOT_IN -> Criteria.where(dbPath).nin(values);
            case EQUALS -> Criteria.where(dbPath).is(parsedValue);
            case NOT_EQUALS -> Criteria.where(dbPath).ne(parsedValue);
            case GREATER_THAN, AFTER -> Criteria.where(dbPath).gt(parsedValue);
            case LESS_THAN, BEFORE -> Criteria.where(dbPath).lt(parsedValue);
            case GREATER_THAN_OR_EQUAL -> Criteria.where(dbPath).gte(parsedValue);
            case LESS_THAN_OR_EQUAL -> Criteria.where(dbPath).lte(parsedValue);
            default -> {
                log.warn("Unsupported filter operator received: {}", operator);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported filter operator: " + operator);
            }
        };
    }

    private Object parseValueForField(String field, String value) {
        if (!FIELD_TYPE_MAP.containsKey(field)) {
            return value;
        }
        try {
            Class<?> type = FIELD_TYPE_MAP.get(field);
            if (type.equals(BigDecimal.class)) {
                return Double.valueOf(value);
            } else if (type.equals(Instant.class)) {
                return Instant.parse(value + "T00:00:00Z");
            }
        } catch (Exception e) {
            log.error("Failed to parse value '{}' for field '{}'", value, field, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid value format for field " + field);
        }
        return value;
    }

    @Override
    public TopSellingProductsApiResponse generateTopSellingReport(Instant startDate, Instant endDate) {
        log.info("Generating top selling products report from {} to {}", startDate, endDate);

        Query query = new Query(Criteria.where("createdAt").gte(startDate).lt(endDate)
                .and("paymentStatus").is(PaymentStatus.COMPLETED));
        List<Order> completedOrders = mongoTemplate.find(query, Order.class);

        Map<String, TopSellingAccumulator> salesData = new HashMap<>();
        for (Order order : completedOrders) {
            for (OrderLineItem lineItem : order.getLineItems()) {
                if (lineItem.getItemType() == LineItemType.COMPONENT) {
                    accumulate(salesData, lineItem.getComponentId(), lineItem.getQuantity(), lineItem.getUnitPrice());
                } else if (lineItem.getItemType() == LineItemType.BUILD && lineItem.getContainedItems() != null) {
                    for (OrderItemSnapshot part : lineItem.getContainedItems()) {
                        long totalPartQuantity = (long) part.getQuantity() * lineItem.getQuantity();
                        accumulate(salesData, part.getComponentId(), totalPartQuantity, part.getPriceAtTimeOfOrder());
                    }
                }
            }
        }

        if (salesData.isEmpty()) {
            return TopSellingProductsApiResponse.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .products(Collections.emptyList())
                    .build();
        }

        List<String> componentIds = new ArrayList<>(salesData.keySet());
        Map<String, Component> componentMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Function.identity()));

        List<TopSellingProductReportDto> topSellingDtos = salesData.entrySet().stream()
                .map(entry -> {
                    String componentId = entry.getKey();
                    TopSellingAccumulator accumulator = entry.getValue();
                    Component component = componentMap.get(componentId);

                    return TopSellingProductReportDto.builder()
                            .productId(componentId)
                            .productName(component != null ? component.getName() : "Unknown Product")
                            .mpn(component != null ? component.getMpn() : "N/A")
                            .totalQuantitySold(accumulator.getTotalQuantitySold())
                            .totalRevenueGenerated(accumulator.getTotalRevenueGenerated())
                            .build();
                })
                .sorted(Comparator.comparing(TopSellingProductReportDto::getTotalRevenueGenerated).reversed())
                .collect(Collectors.toList());

        return TopSellingProductsApiResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .products(topSellingDtos)
                .build();
    }

    @Override
    public LowStockReportApiResponse generateLowStockReport() {
        log.info("Generating low stock report for threshold < {}", LOW_STOCK_THRESHOLD);

        Query query = new Query(Criteria.where("quantity").lt(LOW_STOCK_THRESHOLD).gt(0));
        List<Inventory> lowStockInventories = mongoTemplate.find(query, Inventory.class);

        if (lowStockInventories.isEmpty()) {
            return LowStockReportApiResponse.builder().products(Collections.emptyList()).build();
        }

        List<String> componentIds = lowStockInventories.stream()
                .map(Inventory::getComponentId)
                .collect(Collectors.toList());

        Map<String, Component> componentMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Function.identity()));

        List<LowStockProductReportDto> lowStockDtos = lowStockInventories.stream()
                .map(inventory -> {
                    Component component = componentMap.get(inventory.getComponentId());
                    if (component == null) {
                        return null;
                    }
                    return LowStockProductReportDto.builder()
                            .componentId(component.getId())
                            .componentName(component.getName())
                            .mpn(component.getMpn())
                            .quantityRemaining(inventory.getQuantity())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(LowStockProductReportDto::getQuantityRemaining))
                .collect(Collectors.toList());

        return LowStockReportApiResponse.builder().products(lowStockDtos).build();
    }

    @Override
    public List<String> getPaymentStatuses() {
        log.info("Fetching all possible PaymentStatus values for lookups.");
        return Arrays.stream(PaymentStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPaymentMethods() {
        log.info("Fetching all possible PaymentMethod values for lookups.");
        return Arrays.stream(PaymentMethod.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }

    // --- NEW CSV GENERATION METHODS ---

    @Override
    public String generateOrderReportCsv(OrderSearchRequest request) {
        OrderSearchApiResponse data = this.searchOrders(request);
        return this.orderReportToCsv(data.getOrders());
    }

    @Override
    public String generateTopSellingReportCsv(Instant startDate, Instant endDate) {
        TopSellingProductsApiResponse data = this.generateTopSellingReport(startDate, endDate);
        return this.topSellingReportToCsv(data.getProducts());
    }

    @Override
    public String generateLowStockReportCsv() {
        LowStockReportApiResponse data = this.generateLowStockReport();
        return this.lowStockReportToCsv(data.getProducts());
    }


    // --- PRIVATE UTILITIES MOVED FROM CONTROLLER ---

    private String orderReportToCsv(List<OrderReportDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("OrderID,OrderDate,LastUpdateDate,CustomerName,CustomerEmail,TotalAmount,PaymentMethod,OrderStatus,PaymentStatus\n");
        for (OrderReportDto dto : data) {
            sb.append(String.format("%s,%s,%s,\"%s\",%s,%.2f,%s,%s,%s\n",
                    dto.getOrderId(), dto.getOrderDate(), dto.getLastUpdateDate(), escapeCsv(dto.getCustomerName()),
                    escapeCsv(dto.getCustomerEmail()), dto.getTotalAmount(), dto.getPaymentMethod(),
                    dto.getOrderStatus(), dto.getPaymentStatus()));
        }
        return sb.toString();
    }

    private String topSellingReportToCsv(List<TopSellingProductReportDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("ProductID,ProductName,MPN,TotalQuantitySold,TotalRevenueGenerated\n");
        for (TopSellingProductReportDto dto : data) {
            sb.append(String.format("%s,\"%s\",%s,%d,%.2f\n",
                    dto.getProductId(), escapeCsv(dto.getProductName()), escapeCsv(dto.getMpn()),
                    dto.getTotalQuantitySold(), dto.getTotalRevenueGenerated()));
        }
        return sb.toString();
    }

    private String lowStockReportToCsv(List<LowStockProductReportDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("ComponentID,ComponentName,MPN,QuantityRemaining\n");
        for (LowStockProductReportDto dto : data) {
            sb.append(String.format("%s,\"%s\",%s,%d\n",
                    dto.getComponentId(), escapeCsv(dto.getComponentName()), escapeCsv(dto.getMpn()),
                    dto.getQuantityRemaining()));
        }
        return sb.toString();
    }

    private String escapeCsv(String data) {
        if (data == null) {
            return "";
        }
        return data.replace("\"", "\"\"");
    }


    // --- PRIVATE HELPERS FOR DATA FETCHING ---

    private void accumulate(Map<String, TopSellingAccumulator> map, String componentId, long quantity, BigDecimal price) {
        if (componentId == null || quantity <= 0 || price == null) return;
        TopSellingAccumulator accumulator = map.computeIfAbsent(componentId, k -> new TopSellingAccumulator());
        accumulator.add(quantity, price);
    }

    private Map<String, UserEntity> getUsersForOrders(List<Order> orders) {
        List<String> userIds = orders.stream()
                .map(Order::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    @Data
    private static class TopSellingAccumulator {
        private long totalQuantitySold = 0;
        private BigDecimal totalRevenueGenerated = BigDecimal.ZERO;

        void add(long quantity, BigDecimal price) {
            this.totalQuantitySold += quantity;
            this.totalRevenueGenerated = this.totalRevenueGenerated.add(price.multiply(BigDecimal.valueOf(quantity)));
        }
    }
}