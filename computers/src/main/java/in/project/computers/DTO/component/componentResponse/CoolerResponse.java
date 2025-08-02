package in.project.computers.DTO.component.componentResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CoolerResponse extends ComponentResponse {
    private List<String> socket_support;
    private int height_mm;
    private int radiatorSize_mm;
    private int wattage;
}