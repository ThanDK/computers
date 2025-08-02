package in.project.computers.DTO.cart.cartRequest;

import in.project.computers.entity.order.LineItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddItemToCartRequest {
    @NotBlank(message = "Product ID cannot be blank.")
    private String productId;

    @NotNull(message = "Item type must be specified (BUILD or COMPONENT).")
    private LineItemType itemType;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private int quantity;
}