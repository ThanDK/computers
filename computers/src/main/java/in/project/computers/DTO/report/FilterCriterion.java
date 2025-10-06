package in.project.computers.DTO.report;

import lombok.Data;
import java.util.List;

@Data
public class FilterCriterion {
    private String field;
    private FilterOperator operator;
    private List<String> values;
}