package in.project.computers.entity.component;
import in.project.computers.entity.lookup.FormFactor;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.DBRef;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Psu extends Component {
    private int wattage;
    @DBRef
    private FormFactor formFactor;
}