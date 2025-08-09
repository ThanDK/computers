package in.project.computers.DTO.component.componentResponse;

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
public class StorageDriveResponse extends ComponentResponse {
    private String storage_interface;
    private int capacity_gb;
    private String form_factor;
}