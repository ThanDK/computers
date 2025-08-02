package in.project.computers.entity.component;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Gpu extends Component {
    private int wattage;
    private int length_mm;
}
