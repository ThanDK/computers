package in.project.computers.dto.component.componentResponse;

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
public class RamKitResponse extends ComponentResponse {
    private String ram_type;
    private int ram_size_gb;
    private int moduleCount;
    private int wattage;
}