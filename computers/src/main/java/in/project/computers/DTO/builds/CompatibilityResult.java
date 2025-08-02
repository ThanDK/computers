package in.project.computers.DTO.builds;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompatibilityResult {
    private boolean isCompatible;
    private List<String> errors;
    private List<String> warnings;
    private int totalWattage;
}
