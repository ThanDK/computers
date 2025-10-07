package in.project.computers.DTO.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockConflictInfo {
    private String componentId;
    private String componentName;
    private int requested;
    private int available;
}