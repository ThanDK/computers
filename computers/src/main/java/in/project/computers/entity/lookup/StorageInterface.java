package in.project.computers.entity.lookup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "storageInterfaces")
@AllArgsConstructor
@NoArgsConstructor
public class StorageInterface {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name; // e.g., "SATA III", "M.2", "PCIe 5.0 x4"
}