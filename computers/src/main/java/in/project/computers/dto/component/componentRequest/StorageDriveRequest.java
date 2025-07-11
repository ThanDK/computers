package in.project.computers.dto.component.componentRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StorageDriveRequest extends ComponentRequest {
    private String storage_interface;
    private int capacity_gb; // ADDED
    private String form_factor; // ADDED
}