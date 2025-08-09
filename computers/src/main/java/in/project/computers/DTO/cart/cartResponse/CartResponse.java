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
    private int cartIconCount;
    private int totalProductCount;
}