package in.project.computers.entity.order;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ShippingDetails {

    private String shippingProvider;

    private String shippingProviderLogoUrl; // NEW FIELD

    private String trackingNumber;

    private Instant shippedAt;
}