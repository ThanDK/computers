package in.project.computers.DTO.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    private String id;

    @NotBlank(message = "Contact name is required")
    @Size(min = 2, max = 100, message = "Contact name must be between 2 and 100 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s.'-]+$", message = "Contact name contains invalid characters")
    private String contactName;

    @NotBlank(message = "Phone number is required")
    @Size(min = 9, max = 15, message = "Phone number must be between 9 and 15 digits")
    @Pattern(regexp = "^[0-9+-]+$", message = "Phone number must contain only digits, '+', or '-'")
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 255, message = "Address line 1 is too long")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s-/.#,'()]+$", message = "Address line 1 contains invalid characters")
    private String line1;

    @Size(max = 255, message = "Address line 2 is too long")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s-/.#,'()]*$", message = "Address line 2 contains invalid characters")
    private String line2;

    @NotBlank(message = "Subdistrict is required")
    @Size(max = 100, message = "Subdistrict is too long")
    @Pattern(regexp = "^[\\p{L}\\s-]+$", message = "Subdistrict contains invalid characters")
    private String subdistrict;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District is too long")
    @Pattern(regexp = "^[\\p{L}\\s-]+$", message = "District contains invalid characters")
    private String district;

    @NotBlank(message = "Province is required")
    @Size(max = 100, message = "Province is too long")
    @Pattern(regexp = "^[\\p{L}\\s-]+$", message = "Province contains invalid characters")
    private String province;

    @NotBlank(message = "Zip code is required")
    @Size(min = 5, max = 10, message = "Zip code must be between 5 and 10 characters")
    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "Zip code contains invalid characters")
    private String zipCode;

    @Size(max = 100, message = "Country is too long")
    @Pattern(regexp = "^[\\p{L}\\s-]*$", message = "Country contains invalid characters")
    private String country;

    @JsonProperty("isDefault")
    private boolean isDefault;
}
