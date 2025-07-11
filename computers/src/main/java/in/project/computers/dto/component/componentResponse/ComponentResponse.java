package in.project.computers.dto.component.componentResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CpuResponse.class, name = "cpu"),
        @JsonSubTypes.Type(value = MotherboardResponse.class, name = "motherboard"),
        @JsonSubTypes.Type(value = RamKitResponse.class, name = "ram"),
        @JsonSubTypes.Type(value = GpuResponse.class, name = "gpu"),
        @JsonSubTypes.Type(value = PsuResponse.class, name = "psu"),
        @JsonSubTypes.Type(value = CaseResponse.class, name = "case"),
        @JsonSubTypes.Type(value = CoolerResponse.class, name = "cooler"),
        @JsonSubTypes.Type(value = StorageDriveResponse.class, name = "storage")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ComponentResponse {
    private String id;
    private String mpn;
    @JsonProperty("isActive")
    private boolean isActive;
    private String type;
    private String name;
    private String description;
    private String imageUrl;
    //For Inventory only
    private Integer quantity;
    private BigDecimal price;
}