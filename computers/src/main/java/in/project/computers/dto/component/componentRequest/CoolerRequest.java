package in.project.computers.dto.component.componentRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CoolerRequest extends ComponentRequest {
    private List<String> socket_support;
    private int height_mm;
    private int radiatorSize_mm;
    private int wattage;
}
