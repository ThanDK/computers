package in.project.computers.DTO.component.componentResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class MotherboardResponse extends ComponentResponse {
    private String socket;
    private String ram_type;
    private int max_ram_gb;
    private String form_factor;
    private int pcie_x16_slot_count;
    private int m2_slot_count;
    private int ram_slot_count;
    private int sata_port_count;
    private int wattage;
}