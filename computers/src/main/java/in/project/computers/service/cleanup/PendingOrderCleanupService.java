package in.project.computers.service.cleanup;

import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.OrderStatus;
import in.project.computers.entity.order.PaymentStatus;
import in.project.computers.repository.generalReposiroty.OrderRepository;
import in.project.computers.service.orderService.OrderHelperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PendingOrderCleanupService {

    private final OrderRepository orderRepository;
    private final OrderHelperService orderHelperService;

    // FOR TESTING:
    // @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)

    // PRODUCTION:
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void cleanupAbandonedOrders() {
        Instant now = Instant.now();
        log.info("Running cleanup job for abandoned pending orders at {}", now);

        List<Order> abandonedOrders = orderRepository.findAbandonedPendingOrders(now);

        if (abandonedOrders.isEmpty()) {
            log.info("No abandoned pending orders found.");
            return;
        }

        log.info("Found {} abandoned orders to process.", abandonedOrders.size());

        for (Order order : abandonedOrders) {
            try {
                if (order.getOrderStatus() == OrderStatus.PENDING_PAYMENT) {
                    log.info("Processing abandoned order ID: {}. Releasing stock and cancelling.", order.getId());

                    orderHelperService.incrementStockForOrder(order);

                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    order.setUpdatedAt(now);

                    orderRepository.save(order);
                    log.info("Successfully cancelled abandoned order ID: {}", order.getId());
                } else {
                    log.warn("Skipping order ID: {} as its status changed during processing. Current status: {}", order.getId(), order.getOrderStatus());
                }
            } catch (Exception e) {
                log.error("Failed to process abandoned order ID: {}. Error: {}", order.getId(), e.getMessage(), e);
            }
        }
    }
}