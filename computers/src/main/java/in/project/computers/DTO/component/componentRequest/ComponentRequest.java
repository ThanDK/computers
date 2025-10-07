package in.project.computers.DTO.component.componentRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
        @JsonSubTypes.Type(value = CpuRequest.class, name = "cpu"),
        @JsonSubTypes.Type(value = MotherboardRequest.class, name = "motherboard"),
        @JsonSubTypes.Type(value = RamKitRequest.class, name = "ram"),
        @JsonSubTypes.Type(value = GpuRequest.class, name = "gpu"),
        @JsonSubTypes.Type(value = PsuRequest.class, name = "psu"),
        @JsonSubTypes.Type(value = CaseRequest.class, name = "case"),
        @JsonSubTypes.Type(value = CoolerRequest.class, name = "cooler"),
        @JsonSubTypes.Type(value = StorageDriveRequest.class, name = "storage")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class ComponentRequest {

    @NotBlank(message = "MPN is required")
    private String mpn;

    @NotBlank(message = "Component type is required")
    private String type;

    @NotBlank(message = "Component name is required")
    private String name;

    private String description;

    @NotBlank(message = "Brand ID is required")
    private String brandId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be zero or greater")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be zero or greater")
    private BigDecimal price;
}