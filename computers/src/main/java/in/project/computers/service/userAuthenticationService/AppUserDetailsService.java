package in.project.computers.service.userAuthenticationService;

import in.project.computers.entity.user.UserEntity;
import in.project.computers.repository.generalReposiroty.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return buildUserDetails(user);
    }

    public UserDetails buildUserDetails(UserEntity user) {
        if (user == null) {
            throw new IllegalArgumentException("UserEntity cannot be null");
        }
        if (user.isLocked()) {
            throw new LockedException("User account is locked");
        }
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(user.getRole()));
        String password = user.getPassword() != null ? user.getPassword() : "";

        return new User(user.getEmail(), password, authorities);
    }
}