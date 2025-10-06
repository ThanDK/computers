package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LowStockProductReportDto {
    private String componentId;
    private String componentName;
    private String mpn;
    private Integer quantityRemaining;
}