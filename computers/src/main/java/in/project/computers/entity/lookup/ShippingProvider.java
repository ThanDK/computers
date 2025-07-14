package in.project.computers.entity.lookup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "lookup_shipping_providers")
@AllArgsConstructor
@NoArgsConstructor
public class ShippingProvider {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String imageUrl;

    private String trackingUrl;
}