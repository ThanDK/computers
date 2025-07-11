package in.project.computers.dto.component.componentResponse;

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
public class CaseResponse extends ComponentResponse {
    private List<String> motherboard_form_factor_support;
    private List<String> psu_form_factor_support;
    private List<Integer> supportedRadiatorSizesMm;
    private int max_gpu_length_mm;
    private int max_cooler_height_mm;
    private int bays_2_5_inch;
    private int bays_3_5_inch;
}