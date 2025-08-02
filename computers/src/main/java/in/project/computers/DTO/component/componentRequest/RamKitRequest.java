package in.project.computers.DTO.component.componentRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class RamKitRequest extends ComponentRequest {
    private String ram_type;
    private int ram_size_gb;
    private int moduleCount;
    private int wattage;
}
