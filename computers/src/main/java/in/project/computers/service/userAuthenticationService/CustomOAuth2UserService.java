package in.project.computers.service.userAuthenticationService;

import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalReposiroty.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;
    private final AppUserDetailsService appUserDetailsService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");

        UserEntity userEntity = findOrCreateUser(email, name);

        UserDetails userDetails = appUserDetailsService.buildUserDetails(userEntity);
        log.info("OIDC User: Built UserDetails for '{}' with roles {}", userDetails.getUsername(), userDetails.getAuthorities());

        return new DefaultOidcUser(userDetails.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private UserEntity findOrCreateUser(String email, String name) {
        log.info("OIDC User: Searching for user with email: {}", email);
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .role("ROLE_USER")
                    .password(null)
                    .build();

            try {
                UserEntity savedUser = userRepository.save(newUser);
                log.info("OIDC User: Successfully saved new user with ID: {}", savedUser.getId());
                return savedUser;

            } catch (Exception e) {
                log.error("OIDC User: CRITICAL - FAILED TO SAVE NEW USER TO DATABASE!", e);
                OAuth2Error error = new OAuth2Error("server_error", "The application was unable to save the new user.", null);
                throw new OAuth2AuthenticationException(error, e);
            }
        }
    }
}