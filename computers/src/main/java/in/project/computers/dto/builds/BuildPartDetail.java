package in.project.computers.dto.builds;

import in.project.computers.dto.component.componentResponse.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildPartDetail<T extends ComponentResponse> {
    private T partDetails;
    private int quantity;
}