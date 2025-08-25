package in.project.computers.service.cartService;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.entity.order.Cart;

public interface CartService {

    /**
     * ดึงข้อมูลตะกร้าสินค้าของผู้ใช้ปัจจุบัน
     * @return ข้อมูลตะกร้าสินค้า
     */
    CartResponse getCartForCurrentUser();

    /**
     * เพิ่มสินค้าลงในตะกร้า
     * @param request ข้อมูลสินค้าและจำนวนที่จะเพิ่ม
     * @return ข้อมูลตะกร้าที่อัปเดตแล้ว
     */
    CartResponse addItemToCart(AddItemToCartRequest request);

    /**
     * อัปเดตจำนวนสินค้าในตะกร้า
     * @param cartItemId ID ของรายการในตะกร้า
     * @param request    ข้อมูลจำนวนใหม่
     * @return ข้อมูลตะกร้าที่อัปเดตแล้ว
     */
    CartResponse updateItemInCart(String cartItemId, UpdateCartItemRequest request);

    /**
     * ลบสินค้าออกจากตะกร้า
     * @param cartItemId ID ของรายการที่จะลบ
     * @return ข้อมูลตะกร้าที่อัปเดตแล้ว
     */
    CartResponse removeItemFromCart(String cartItemId);

    /**
     * ล้างตะกร้าสินค้าของผู้ใช้
     * @param userId ID ของผู้ใช้
     */
    void clearCart(String userId);

    /**
     * ดึง Cart entity ของผู้ใช้
     * @param userId ID ของผู้ใช้
     * @return อ็อบเจ็กต์ Cart entity
     */
    Cart getCartEntityByUserId(String userId);
}