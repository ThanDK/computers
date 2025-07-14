package in.project.computers.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShippingProviderRequest {

    @NotBlank(message = "Provider name cannot be blank")
    private String name;

    // These can be optional, so no @NotBlank
    private String imageUrl;

    private String trackingUrl;
}