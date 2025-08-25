package in.project.computers.entity.component;

import in.project.computers.entity.lookup.FormFactor;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.List;

@Data
@SuperBuilder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Case extends Component {
    @DBRef
    private List<FormFactor> supportedFormFactors;
    @DBRef
    private List<FormFactor> supportedPsuFormFactors;
    private int max_gpu_length_mm;
    private int max_cooler_height_mm;
    private int bays_2_5_inch;
    private int bays_3_5_inch;
    private List<Integer> supportedRadiatorSizesMm;
}