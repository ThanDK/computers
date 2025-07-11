package in.project.computers.service.userAuthenticationService;
import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {
    Authentication getAuthentication();

}