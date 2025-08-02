package in.project.computers.DTO.builds;

import in.project.computers.DTO.component.componentResponse.*;
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