// in/project/computers/entity/lookup/FormFactor.java
package in.project.computers.entity.lookup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "lookup_form_factors")
@AllArgsConstructor
@NoArgsConstructor
@CompoundIndex(name = "name_type_idx", def = "{'name' : 1, 'type': 1}", unique = true)
public class FormFactor {
    @Id
    private String id;
    private String name;
    private FormFactorType type;
}