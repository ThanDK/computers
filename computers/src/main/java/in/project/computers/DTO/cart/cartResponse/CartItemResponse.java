package in.project.computers.DTO.cart.cartResponse;

import in.project.computers.entity.order.LineItemType;
import in.project.computers.entity.order.OrderItemSnapshot;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
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
    private BigDecimal lineTotal;
    private String imageUrl;
    private List<OrderItemSnapshot> containedItemsSnapshot;

    // --- NEW FIELDS FOR API RESPONSE ---
    private boolean stockReserved;
    private Instant reservationExpiresAt;
}