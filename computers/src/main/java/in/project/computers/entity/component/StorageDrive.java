package in.project.computers.entity.component;

import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.StorageInterface;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StorageDrive extends Component {
    @DBRef
    private StorageInterface storageInterface;
    private int capacity_gb;
    @DBRef
    private FormFactor formFactor;
}