package in.project.computers.DTO.component.componentRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class CaseRequest extends ComponentRequest {
    private List<String> motherboard_form_factor_support;
    private List<String> psu_form_factor_support;
    private List<Integer> supportedRadiatorSizesMm;
    private int max_gpu_length_mm;
    private int max_cooler_height_mm;
    private int bays_2_5_inch;
    private int bays_3_5_inch;
}
