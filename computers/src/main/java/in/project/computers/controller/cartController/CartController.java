package in.project.computers.controller.cartController;

import in.project.computers.dto.cart.AddItemToCartRequest;
import in.project.computers.dto.cart.CartResponse;
import in.project.computers.dto.cart.UpdateCartItemRequest;
import in.project.computers.service.cartService.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;


    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        log.info("Request received to get current user's cart.");
        return ResponseEntity.ok(cartService.getCartForCurrentUser());
    }


    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody AddItemToCartRequest request) {
        log.info("Request received to add item {} to cart.", request.getProductId());
        return ResponseEntity.ok(cartService.addItemToCart(request));
    }


    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Request received to update cart item {} to quantity {}.", cartItemId, request.getQuantity());
        return ResponseEntity.ok(cartService.updateItemInCart(cartItemId, request));
    }


    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable String cartItemId) {
        log.info("Request received to remove cart item {}.", cartItemId);
        return ResponseEntity.ok(cartService.removeItemFromCart(cartItemId));
    }
}