package in.project.computers.dto.component.componentRequest;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class MotherboardRequest extends ComponentRequest {
    @NotBlank
    private String socket;
    @NotBlank
    private String ram_type;
    @NotBlank
    private String form_factor;
    @Min(1)
    private int max_ram_gb;
    @Min(0)
    private int pcie_x16_slot_count;
    @Min(0)
    private int m2_slot_count;
    @Min(1)
    private int ram_slot_count;    // <-- ADD THIS
    @Min(0)
    private int sata_port_count;   // <-- ADD THIS
    @Min(0)
    private int wattage;
}