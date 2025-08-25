package in.project.computers.entity.component;

import in.project.computers.entity.lookup.RamType;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RamKit extends Component {
    private RamType ramType;
    private int ram_size_gb;
    private int moduleCount;
    private int wattage;
}