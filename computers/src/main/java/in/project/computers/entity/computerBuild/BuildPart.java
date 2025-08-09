package in.project.computers.entity.computerBuild;

import in.project.computers.entity.component.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildPart<T extends Component> {
    private T component;
    private int quantity;
}