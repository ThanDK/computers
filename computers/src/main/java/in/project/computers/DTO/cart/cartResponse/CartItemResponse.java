package in.project.computers.DTO.cart.cartResponse;

import in.project.computers.entity.order.LineItemType;
import in.project.computers.entity.order.OrderItemSnapshot;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartItemResponse {
    private String cartItemId;
    private String productId;
    private String name;
    private int quantity;
    private LineItemType itemType;
    private BigDecimal unitPrice;
    private String imageUrl;
    private BigDecimal lineTotal;
    private List<OrderItemSnapshot> containedItemsSnapshot;
}