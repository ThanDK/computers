package in.project.computers.service.cartService;

import in.project.computers.DTO.cart.cartRequest.AddItemToCartRequest;
import in.project.computers.DTO.cart.cartResponse.CartResponse;
import in.project.computers.DTO.cart.cartRequest.UpdateCartItemRequest;
import in.project.computers.entity.order.Cart;

/**
 * Interface สำหรับบริการจัดการตะกร้าสินค้า (Shopping Cart)
 * <p>
 * กำหนดสัญญา (contract) สำหรับการดำเนินการต่างๆ ที่เกี่ยวกับตะกร้าสินค้าของผู้ใช้
 * เช่น การเพิ่ม, แก้ไข, ลบรายการสินค้า และการดึงข้อมูลตะกร้า
 */
public interface CartService {

    /**
     * ดึงข้อมูลตะกร้าสินค้าของผู้ใช้ที่ล็อกอินอยู่ปัจจุบัน
     *
     * @return ข้อมูลตะกร้าสินค้าในรูปแบบ CartResponse ซึ่งเหมาะสำหรับส่งไปให้ Frontend
     */
    CartResponse getCartForCurrentUser();

    /**
     * เพิ่มสินค้าลงในตะกร้าสินค้าของผู้ใช้ปัจจุบัน
     * <p>
     * หากสินค้านั้นมีอยู่แล้วในตะกร้า จะทำการบวกจำนวนเพิ่มเข้าไป
     *
     * @param request ข้อมูลของสินค้า (ID) และจำนวนที่ต้องการเพิ่ม
     * @return ข้อมูลตะกร้าสินค้าที่อัปเดตแล้ว
     */
    CartResponse addItemToCart(AddItemToCartRequest request);

    /**
     * อัปเดตจำนวนของสินค้าที่มีอยู่แล้วในตะกร้า
     *
     * @param cartItemId ID ของรายการสินค้าในตะกร้า (CartItem) ที่ต้องการอัปเดต
     * @param request    ข้อมูลจำนวนใหม่ที่ต้องการ
     * @return ข้อมูลตะกร้าสินค้าที่อัปเดตแล้ว
     */
    CartResponse updateItemInCart(String cartItemId, UpdateCartItemRequest request);

    /**
     * ลบสินค้าออกจากตะกร้า
     *
     * @param cartItemId ID ของรายการสินค้าในตะกร้า (CartItem) ที่ต้องการลบ
     * @return ข้อมูลตะกร้าสินค้าที่อัปเดตแล้ว
     */
    CartResponse removeItemFromCart(String cartItemId);

    /**
     * ล้างตะกร้าสินค้าของผู้ใช้ทั้งหมด (ลบ CartItem ทุกรายการ)
     * <p>
     * มักถูกเรียกใช้หลังจากที่ผู้ใช้ได้สร้างคำสั่งซื้อสำเร็จแล้ว
     *
     * @param userId ID ของผู้ใช้ที่ต้องการล้างตะกร้า
     */
    void clearCart(String userId);

    /**
     * ดึงข้อมูลตะกร้าสินค้าในรูปแบบของ Entity โดยตรงจาก ID ของผู้ใช้
     * <p>
     * เมธอดนี้มีไว้สำหรับใช้ภายใน Service อื่นๆ (เช่น OrderService) ที่ต้องการอ้างอิงถึง Cart entity
     *
     * @param userId ID ของผู้ใช้
     * @return อ็อบเจ็กต์ Cart entity ที่เชื่อมกับผู้ใช้นั้น
     */
    Cart getCartEntityByUserId(String userId);
}