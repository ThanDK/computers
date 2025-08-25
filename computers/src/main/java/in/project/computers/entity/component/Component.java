package in.project.computers.entity.component;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import in.project.computers.entity.lookup.Brand;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Cpu.class, name = "cpu"),
        @JsonSubTypes.Type(value = Motherboard.class, name = "motherboard"),
        @JsonSubTypes.Type(value = RamKit.class, name = "ram"),
        @JsonSubTypes.Type(value = Gpu.class, name = "gpu"),
        @JsonSubTypes.Type(value = Psu.class, name = "psu"),
        @JsonSubTypes.Type(value = Case.class, name = "case"),
        @JsonSubTypes.Type(value = Cooler.class, name = "cooler"),
        @JsonSubTypes.Type(value = StorageDrive.class, name = "storage")
})
@Data
@SuperBuilder
@Accessors(chain = true)
@Document(collection = "component")
@AllArgsConstructor
@NoArgsConstructor
public abstract class Component {

    @Id
    private String id;

    private String mpn;

    private boolean isActive;

    private String type;

    private String name;

    private String description;

    private String imageUrl;

    @DBRef
    private Brand brand;
}