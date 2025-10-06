package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LowStockReportApiResponse {
    private List<LowStockProductReportDto> products;
}