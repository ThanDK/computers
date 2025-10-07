package in.project.computers.service.cartService;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.DTO.cart.cartResponse.CartItemResponse;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.DTO.inventory.StockConflictInfo;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.order.Cart;
import in.project.computers.entity.order.CartItem;
import in.project.computers.entity.order.LineItemType;
import in.project.computers.entity.order.OrderItemSnapshot;
import in.project.computers.exception.StockConflictException;
import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.repository.generalReposiroty.CartRepository;
import in.project.computers.repository.generalReposiroty.ComputerBuildRepository;
import in.project.computers.service.userAuthenticationService.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final UserService userService;
    private final ComponentRepository componentRepository;
    private final ComputerBuildRepository buildRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    public CartResponse getCartForCurrentUser() {
        String userId = userService.findByUserId();
        Cart cart = findOrCreateCartByUserId(userId);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(AddItemToCartRequest request) {
        String userId = userService.findByUserId();
        Cart cart = findOrCreateCartByUserId(userId);
        int requestedQuantity = request.getQuantity();

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + requestedQuantity;

            Map<String, Integer> requiredStock = getRequiredStockForCartItem(existingItem.getProductId(), existingItem.getItemType(), requestedQuantity);
            List<StockConflictInfo> conflicts = inventoryRepository.attemptReservation(requiredStock);
            if (!conflicts.isEmpty()) {
                throw new StockConflictException(conflicts);
            }

            existingItem.setQuantity(newQuantity);
            log.info("Reserved additional {} units for item {} in cart for user {}", requestedQuantity, request.getProductId(), userId);
        } else {
            Map<String, Integer> requiredStock = getRequiredStockForCartItem(request.getProductId(), request.getItemType(), requestedQuantity);
            List<StockConflictInfo> conflicts = inventoryRepository.attemptReservation(requiredStock);
            if (!conflicts.isEmpty()) {
                throw new StockConflictException(conflicts);
            }

            CartItem newItem = createNewCartItem(request);
            newItem.setStockReserved(true);
            newItem.setReservationExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
            cart.getItems().add(newItem);
            log.info("Added new item {} (type: {}) and reserved its stock for user {}", request.getProductId(), request.getItemType(), userId);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateItemInCart(String cartItemId, UpdateCartItemRequest request) {
        String userId = userService.findByUserId();
        Cart cart = getCartEntityByUserId(userId);
        CartItem itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in cart."));

        int currentQuantity = itemToUpdate.getQuantity();
        int newQuantity = request.getQuantity();
        int quantityDifference = newQuantity - currentQuantity;

        if (quantityDifference > 0) {
            Map<String, Integer> requiredStock = getRequiredStockForCartItem(itemToUpdate.getProductId(), itemToUpdate.getItemType(), quantityDifference);
            List<StockConflictInfo> conflicts = inventoryRepository.attemptReservation(requiredStock);
            if (!conflicts.isEmpty()) {
                throw new StockConflictException(conflicts);
            }
            log.info("Reserved an additional {} units for cart item {}", quantityDifference, cartItemId);
        } else if (quantityDifference < 0) {
            Map<String, Integer> stockToRelease = getRequiredStockForCartItem(itemToUpdate.getProductId(), itemToUpdate.getItemType(), -quantityDifference);
            inventoryRepository.bulkAtomicUpdateQuantities(stockToRelease);
            log.info("Released {} units for cart item {}", -quantityDifference, cartItemId);
        }

        itemToUpdate.setQuantity(newQuantity);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(String cartItemId) {
        String userId = userService.findByUserId();
        Cart cart = getCartEntityByUserId(userId);
        Optional<CartItem> itemToRemoveOpt = cart.getItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst();

        if (itemToRemoveOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in cart.");
        }

        CartItem itemToRemove = itemToRemoveOpt.get();

        if (itemToRemove.isStockReserved()) {
            Map<String, Integer> stockToRelease = getRequiredStockForCartItem(itemToRemove.getProductId(), itemToRemove.getItemType(), itemToRemove.getQuantity());
            inventoryRepository.bulkAtomicUpdateQuantities(stockToRelease);
            log.info("Released stock for removed cart item {}", cartItemId);
        }

        cart.getItems().remove(itemToRemove);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        Cart cart = getCartEntityByUserId(userId);
        if (cart.getItems().isEmpty()) {
            return;
        }

        Map<String, Integer> stockToRelease = new HashMap<>();
        for (CartItem item : cart.getItems()) {
            if (item.isStockReserved()) {
                Map<String, Integer> itemStock = getRequiredStockForCartItem(item.getProductId(), item.getItemType(), item.getQuantity());
                itemStock.forEach((key, value) -> stockToRelease.merge(key, value, Integer::sum));
            }
        }

        if (!stockToRelease.isEmpty()) {
            inventoryRepository.bulkAtomicUpdateQuantities(stockToRelease);
            log.info("Released all reserved stock while clearing cart for user {}", userId);
        }

        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    @Override
    public Cart getCartEntityByUserId(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found for user."));
    }

    private Cart findOrCreateCartByUserId(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            log.info("No cart found for user {}, creating a new one.", userId);
            Cart newCart = Cart.builder()
                    .userId(userId)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            return cartRepository.save(newCart);
        });
    }

    private CartItem createNewCartItem(AddItemToCartRequest request) {
        if (request.getItemType() == LineItemType.COMPONENT) {
            Component component = componentRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found."));
            BigDecimal price = inventoryRepository.findByComponentId(component.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory not found for component."))
                    .getPrice();
            return CartItem.builder()
                    .cartItemId(UUID.randomUUID().toString())
                    .productId(component.getId())
                    .name(component.getName())
                    .quantity(request.getQuantity())
                    .itemType(LineItemType.COMPONENT)
                    .unitPrice(price)
                    .imageUrl(component.getImageUrl())
                    .containedItemsSnapshot(null)
                    .build();
        } else if (request.getItemType() == LineItemType.BUILD) {
            ComputerBuild build = buildRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found."));

            List<OrderItemSnapshot> snapshots = new ArrayList<>();
            final BigDecimal[] totalPrice = {BigDecimal.ZERO};

            forEachComponentInBuild(build, (component, qty) -> {
                BigDecimal priceAtTimeOfAdding = inventoryRepository.findByComponentId(component.getId())
                        .orElseThrow(() -> new IllegalStateException("Inventory missing for component " + component.getId()))
                        .getPrice();

                snapshots.add(OrderItemSnapshot.builder()
                        .componentId(component.getId())
                        .name(component.getName())
                        .mpn(component.getMpn())
                        .quantity(qty)
                        .priceAtTimeOfOrder(priceAtTimeOfAdding)
                        .imageUrl(component.getImageUrl())
                        .build());

                totalPrice[0] = totalPrice[0].add(priceAtTimeOfAdding.multiply(BigDecimal.valueOf(qty)));
            });

            return CartItem.builder()
                    .cartItemId(UUID.randomUUID().toString())
                    .productId(build.getId())
                    .name(build.getBuildName())
                    .quantity(request.getQuantity())
                    .itemType(LineItemType.BUILD)
                    .unitPrice(totalPrice[0])
                    .imageUrl(null)
                    .containedItemsSnapshot(snapshots)
                    .build();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid item type.");
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

    private void forEachComponentInBuild(ComputerBuild build, BiConsumer<Component, Integer> action) {
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

    private CartResponse entityToResponse(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return CartResponse.builder()
                    .id(cart != null ? cart.getId() : null)
                    .userId(cart != null ? cart.getUserId() : null)
                    .items(java.util.Collections.emptyList())
                    .subtotal(BigDecimal.ZERO)
                    .cartIconCount(0)
                    .totalProductCount(0)
                    .build();
        }

        AtomicInteger totalProductCounter = new AtomicInteger(0);

        List<CartItemResponse> itemResponses = cart.getItems().stream().map(item -> {
            totalProductCounter.addAndGet(item.getQuantity());

            return CartItemResponse.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProductId())
                    .name(item.getName())
                    .quantity(item.getQuantity())
                    .itemType(item.getItemType())
                    .unitPrice(item.getUnitPrice())
                    .imageUrl(item.getImageUrl())
                    .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .containedItemsSnapshot(item.getContainedItemsSnapshot())
                    .stockReserved(item.isStockReserved())
                    .reservationExpiresAt(item.getReservationExpiresAt())
                    .build();
        }).collect(Collectors.toList());


        int iconCount = cart.getItems().size();

        int productCount = totalProductCounter.get();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .subtotal(subtotal)
                .cartIconCount(iconCount)
                .totalProductCount(productCount)
                .build();
    }
}