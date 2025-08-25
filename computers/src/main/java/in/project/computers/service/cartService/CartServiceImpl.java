package in.project.computers.service.cartService;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.DTO.cart.cartResponse.CartItemResponse;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.order.Cart;
import in.project.computers.entity.order.CartItem;
import in.project.computers.entity.order.LineItemType;
import in.project.computers.entity.order.OrderItemSnapshot;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
            validateStock(existingItem.getProductId(), existingItem.getItemType(), newQuantity);
            existingItem.setQuantity(newQuantity);
            log.info("Updated quantity for item {} in cart for user {}", request.getProductId(), userId);
        } else {
            validateStock(request.getProductId(), request.getItemType(), requestedQuantity);
            CartItem newItem = createNewCartItem(request);
            cart.getItems().add(newItem);
            log.info("Added new item {} to cart for user {}", request.getProductId(), userId);
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

        validateStock(itemToUpdate.getProductId(), itemToUpdate.getItemType(), request.getQuantity());
        itemToUpdate.setQuantity(request.getQuantity());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        log.info("Updated quantity for cart item {} for user {}", cartItemId, userId);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeItemFromCart(String cartItemId) {
        String userId = userService.findByUserId();
        Cart cart = getCartEntityByUserId(userId);
        boolean removed = cart.getItems().removeIf(item -> item.getCartItemId().equals(cartItemId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in cart.");
        }
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        log.info("Removed cart item {} for user {}", cartItemId, userId);
        return entityToResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(String userId) {
        Cart cart = getCartEntityByUserId(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
        log.info("Cleared all items from the cart for user {}", userId);
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

    private void validateStock(String productId, LineItemType type, int requestedQuantity) {
        log.debug("Validating stock for productId: {}, type: {}, quantity: {}", productId, type, requestedQuantity);
        if (type == LineItemType.COMPONENT) {
            int availableStock = inventoryRepository.findByComponentId(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory for component not found."))
                    .getQuantity();
            if (availableStock < requestedQuantity) {
                String componentName = componentRepository.findById(productId).map(Component::getName).orElse(productId);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for " + componentName);
            }
        } else if (type == LineItemType.BUILD) {
            ComputerBuild build = buildRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found."));
            forEachComponentInBuild(build, (component, qtyInBuild) -> {
                int totalRequired = qtyInBuild * requestedQuantity;
                int availableStock = inventoryRepository.findByComponentId(component.getId())
                        .orElseThrow(() -> new IllegalStateException("Missing inventory for " + component.getId()))
                        .getQuantity();
                if (availableStock < totalRequired) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for '" + component.getName() + "' required for the build.");
                }
            });
        }
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