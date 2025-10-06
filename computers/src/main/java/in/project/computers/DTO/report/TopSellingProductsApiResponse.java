package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class TopSellingProductsApiResponse {
    private Instant startDate;
    private Instant endDate;
    private List<TopSellingProductReportDto> products;
}