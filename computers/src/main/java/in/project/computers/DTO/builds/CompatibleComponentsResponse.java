package in.project.computers.DTO.builds;

import com.fasterxml.jackson.annotation.JsonInclude;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CompatibleComponentsResponse {
    private Map<String, List<ComponentResponse>> availableComponents;
    private int totalWattage;
}