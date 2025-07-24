package in.project.computers.entity.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    @Field("cart_item_id")
    private String cartItemId;

    @Field("product_id")
    private String productId;

    private String name;

    private int quantity;

    @Field("item_type")
    private LineItemType itemType;

    @Field("unit_price")
    private BigDecimal unitPrice;

    @Field("image_url")
    private String imageUrl;


    @Field("contained_items_snapshot")
    private List<OrderItemSnapshot> containedItemsSnapshot;
}