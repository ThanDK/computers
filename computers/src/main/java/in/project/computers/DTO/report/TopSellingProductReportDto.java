package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TopSellingProductReportDto {
    private String productId;
    private String productName;
    private String mpn;
    private Long totalQuantitySold;
    private BigDecimal totalRevenueGenerated;
}