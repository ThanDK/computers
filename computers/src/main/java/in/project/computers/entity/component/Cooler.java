package in.project.computers.entity.component;

import in.project.computers.entity.lookup.Socket;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cooler extends Component {
    private List<Socket> supportedSockets;
    private int height_mm;
    private int radiatorSize_mm;
    private int wattage;
}