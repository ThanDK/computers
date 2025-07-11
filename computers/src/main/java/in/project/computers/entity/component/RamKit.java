package in.project.computers.entity.component;

import in.project.computers.entity.lookup.RamType;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RamKit extends Component {
    private RamType ramType; // CHANGED
    private int ram_size_gb;
    private int moduleCount;
    private int wattage;
}