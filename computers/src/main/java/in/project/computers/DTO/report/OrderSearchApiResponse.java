package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderSearchApiResponse {
    private Instant startDate;
    private Instant endDate;
    private List<OrderReportDto> orders;
}