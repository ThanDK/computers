package in.project.computers.DTO.cart.cartResponse;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private String id;
    private String userId;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    /**
     * The number of distinct line items in the cart.
     * Use this for the shopping cart icon badge.
     * Example: 2 RAM kits and 2 PCs = 2.
     */
    private int cartIconCount;

    /**
     * The total number of all products, accounting for quantities.
     * Use this for display text like "Subtotal (4 items)".
     * Example: 2 RAM kits and 2 PCs = 4.
     */
    private int totalProductCount;
}