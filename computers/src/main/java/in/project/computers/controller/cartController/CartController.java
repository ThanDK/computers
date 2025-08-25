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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService cartService;

    /**
     * ดึงข้อมูลตะกร้าสินค้าของผู้ใช้ปัจจุบัน
     * <p>
     * Endpoint นี้ใช้สำหรับดึงข้อมูลทั้งหมดในตะกร้าสินค้าของผู้ใช้ที่กำลังล็อกอินอยู่
     * รวมถึงรายการสินค้า, จำนวน, ราคา, และยอดรวมทั้งหมด
     * </p>
     * @return ResponseEntity ที่มีข้อมูล {@link CartResponse} ของตะกร้าสินค้าและสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        log.info("Request received to get current user's cart.");
        return ResponseEntity.ok(cartService.getCartForCurrentUser());
    }

    /**
     * เพิ่มสินค้าลงในตะกร้า
     * <p>
     * Endpoint นี้ใช้สำหรับเพิ่มสินค้าชิ้นใหม่ลงในตะกร้า หรือบวกจำนวนสินค้าที่มีอยู่แล้ว
     * ระบบจะตรวจสอบและรวมรายการสินค้าเดียวกันโดยอัตโนมัติ
     * </p>
     * @param request อ็อบเจกต์ {@link AddItemToCartRequest} ที่มี ID ของสินค้าและจำนวนที่ต้องการเพิ่ม
     * @return ResponseEntity ที่มีข้อมูล {@link CartResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(@Valid @RequestBody AddItemToCartRequest request) {
        log.info("Request received to add item {} to cart.", request.getProductId());
        return ResponseEntity.ok(cartService.addItemToCart(request));
    }

    /**
     * อัปเดตจำนวนสินค้าในตะกร้า
     * <p>
     * Endpoint นี้ใช้สำหรับแก้ไขจำนวนของสินค้าที่มีอยู่แล้วในตะกร้าโดยตรง
     * </p>
     * @param cartItemId ID ของรายการสินค้าในตะกร้า (CartItem) ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link UpdateCartItemRequest} ที่มีจำนวนใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link CartResponse} ที่อัปเดตแล้วและสถานะ 200 OK
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
     * <p>
     * Endpoint นี้ใช้สำหรับนำรายการสินค้าออกจากตะกร้าของผู้ใช้ทั้งหมด ไม่ว่าจะมีจำนวนเท่าใดก็ตาม
     * </p>
     * @param cartItemId ID ของรายการสินค้าในตะกร้า (CartItem) ที่ต้องการลบ (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link CartResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable String cartItemId) {
        log.info("Request received to remove cart item {}.", cartItemId);
        return ResponseEntity.ok(cartService.removeItemFromCart(cartItemId));
    }
}