package in.project.computers.entity.component;

import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.RamType;
import in.project.computers.entity.lookup.Socket;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Motherboard extends Component {
    private Socket socket;
    private RamType ramType;
    private FormFactor formFactor;
    private int max_ram_gb;
    private int pcie_x16_slot_count;
    private int m2_slot_count;
    private int ram_slot_count;
    private int sata_port_count;
    private int wattage;
}