package in.project.computers.DTO.report;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class OrderSearchRequest {
    private Instant startDate;
    private Instant endDate;
    private String logic;
    private List<FilterCriterion> filters;
}