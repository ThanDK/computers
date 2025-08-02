package in.project.computers.service.cartService;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.entity.order.Cart;

public interface CartService {
    CartResponse getCartForCurrentUser();
    CartResponse addItemToCart(AddItemToCartRequest request);
    CartResponse updateItemInCart(String cartItemId, UpdateCartItemRequest request);
    CartResponse removeItemFromCart(String cartItemId);
    void clearCart(String userId);
    Cart getCartEntityByUserId(String userId);
}