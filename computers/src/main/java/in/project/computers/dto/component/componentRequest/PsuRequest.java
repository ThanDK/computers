package in.project.computers.dto.component.componentRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class PsuRequest extends ComponentRequest {
        private int wattage;
        private String form_factor; // ADDED
}
