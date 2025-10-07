package in.project.computers.service.cleanup;

import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.order.Cart;
import in.project.computers.entity.order.CartItem;
import in.project.computers.entity.order.LineItemType;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.repository.generalReposiroty.CartRepository;
import in.project.computers.repository.generalReposiroty.ComputerBuildRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class CartReservationCleanupService {

    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;
    private final ComputerBuildRepository buildRepository;


    // FOR TESTING:
    // @Scheduled(fixedRate = 10, timeUnit = TimeUnit.SECONDS)

    // PRODUCTION:
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void cleanupExpiredCartReservations() {
        Instant now = Instant.now();
        log.info("Running cleanup job for expired cart reservations at {}", now);

        List<Cart> cartsWithExpirations = cartRepository.findCartsWithExpiredReservations(now);
        if (cartsWithExpirations.isEmpty()) {
            log.info("No carts with expired reservations found.");
            return;
        }

        Map<String, Integer> stockToRelease = new HashMap<>();
        int itemsExpiredCount = 0;

        for (Cart cart : cartsWithExpirations) {
            boolean cartModified = false;
            for (CartItem item : cart.getItems()) {
                if (item.isStockReserved() && item.getReservationExpiresAt() != null && item.getReservationExpiresAt().isBefore(now)) {
                    log.info("Reservation expired for item {} (product ID: {}) in cart {}", item.getCartItemId(), item.getProductId(), cart.getId());
                    item.setStockReserved(false);

                    Map<String, Integer> itemStock = getRequiredStockForCartItem(item.getProductId(), item.getItemType(), item.getQuantity());
                    itemStock.forEach((key, value) -> stockToRelease.merge(key, value, Integer::sum));

                    itemsExpiredCount++;
                    cartModified = true;
                }
            }
            if (cartModified) {
                cart.setUpdatedAt(now);
                cartRepository.save(cart);
            }
        }

        if (!stockToRelease.isEmpty()) {
            inventoryRepository.bulkAtomicUpdateQuantities(stockToRelease);
            log.info("Successfully released stock for {} expired cart items across {} carts.", itemsExpiredCount, cartsWithExpirations.size());
        } else {
            log.warn("Carts with expired items were found, but no stock was calculated for release. This may indicate an issue.");
        }
    }

    private Map<String, Integer> getRequiredStockForCartItem(String productId, LineItemType type, int quantity) {
        Map<String, Integer> requiredStock = new HashMap<>();
        if (type == LineItemType.COMPONENT) {
            requiredStock.put(productId, quantity);
        } else if (type == LineItemType.BUILD) {
            ComputerBuild build = buildRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found during stock calculation."));
            forEachComponentInBuild(build, (component, qtyInBuild) -> {
                int totalRequired = qtyInBuild * quantity;
                requiredStock.merge(component.getId(), totalRequired, Integer::sum);
            });
        }
        return requiredStock;
    }

    private void forEachComponentInBuild(ComputerBuild build, BiConsumer<in.project.computers.entity.component.Component, Integer> action) {
        Stream.of(build.getCpu(), build.getMotherboard(), build.getPsu(), build.getCaseDetail(), build.getCooler())
                .filter(Objects::nonNull)
                .forEach(component -> action.accept(component, 1));
        if (build.getRamKits() != null) {
            build.getRamKits().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getGpus() != null) {
            build.getGpus().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
        if (build.getStorageDrives() != null) {
            build.getStorageDrives().forEach(part -> action.accept(part.getComponent(), part.getQuantity()));
        }
    }
}