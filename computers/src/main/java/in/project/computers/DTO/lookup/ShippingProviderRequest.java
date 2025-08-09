package in.project.computers.DTO.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingProviderRequest {

    @NotBlank(message = "Provider name cannot be blank")
    private String name;

    private String imageUrl;

    private String trackingUrl;
}