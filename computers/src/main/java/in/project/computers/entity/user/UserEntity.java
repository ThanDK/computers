package in.project.computers.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "users")
@Builder
public class UserEntity {
    @Id
    private String id;
    private String name;
    private String email;
    private String password;
    private String role;
    private String profilePictureUrl;
    @Builder.Default
    private boolean locked = false;

    @Builder.Default
    private List<Address> savedAddresses = new ArrayList<>();
}