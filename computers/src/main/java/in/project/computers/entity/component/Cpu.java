package in.project.computers.entity.component;

import in.project.computers.entity.lookup.Socket;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cpu extends Component {
    private Socket socket; // CHANGED
    private int wattage;
}