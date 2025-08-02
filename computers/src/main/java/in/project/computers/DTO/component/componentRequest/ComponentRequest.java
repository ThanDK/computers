package in.project.computers.DTO.component.componentRequest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true
)
// เลือกจากรูปแบบแล้วไปสร้างตามแบบต่างๆ
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
@AllArgsConstructor
@NoArgsConstructor
public abstract class ComponentRequest {

    private String mpn;
    private boolean isActive;
    private String type;
    private String name;
    private String description;
    private String imageUrl;

    private int quantity;
    private BigDecimal price;

    @NotBlank(message = "Brand ID cannot be blank")
    private String brandId;
}