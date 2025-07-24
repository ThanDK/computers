package in.project.computers.entity.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSnapshot {
    private String componentId;
    private String name;
    private String mpn;
    private int quantity;
    private String imageUrl;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal priceAtTimeOfOrder;

}
