package in.project.computers.entity.component;

import in.project.computers.entity.lookup.Socket;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cpu extends Component {
    private Socket socket;
    private int wattage;
}