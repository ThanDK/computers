package in.project.computers.entity.lookup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "lookup_sockets")
@AllArgsConstructor
@NoArgsConstructor
public class Socket {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name; // e.g., "AM5", "LGA1700"

    private String brand; // e.g., "AMD", "Intel"
}