package in.project.computers.controller.cartController;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.service.cartService.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller สำหรับจัดการตะกร้าสินค้าของผู้ใช้
 * <p>
 * ทุก Endpoint ในคลาสนี้ต้องการการยืนยันตัวตน (Authentication)
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    /**
     * ดึงข้อมูลตะกร้าสินค้าทั้งหมดของผู้ใช้ที่ล็อกอินอยู่
     * @return ข้อมูลตะกร้าสินค้าปัจจุบัน
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        log.info("Request received to get current user's cart.");
        return ResponseEntity.ok(cartService.getCartForCurrentUser());
    }

    /**
     * เพิ่มสินค้าลงในตะกร้า
     * @param request ข้อมูลสินค้า (ID) และจำนวนที่ต้องการเพิ่ม
     * @return ข้อมูลตะกร้าสินค้าหลังการอัปเดต
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody AddItemToCartRequest request) {
        log.info("Request received to add item {} to cart.", request.getProductId());
        return ResponseEntity.ok(cartService.addItemToCart(request));
    }

    /**
     * อัปเดตจำนวนของสินค้าที่มีอยู่แล้วในตะกร้า
     * @param cartItemId ID ของรายการสินค้าในตะกร้า (Cart Item ID)
     * @param request ข้อมูลจำนวนใหม่ที่ต้องการ
     * @return ข้อมูลตะกร้าสินค้าหลังการอัปเดต
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable String cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        log.info("Request received to update cart item {} to quantity {}.", cartItemId, request.getQuantity());
        return ResponseEntity.ok(cartService.updateItemInCart(cartItemId, request));
    }

    /**
     * ลบสินค้าออกจากตะกร้า
     * @param cartItemId ID ของรายการสินค้าในตะกร้าที่จะลบ
     * @return ข้อมูลตะกร้าสินค้าหลังการอัปเดต
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable String cartItemId) {
        log.info("Request received to remove cart item {}.", cartItemId);
        return ResponseEntity.ok(cartService.removeItemFromCart(cartItemId));
    }
}