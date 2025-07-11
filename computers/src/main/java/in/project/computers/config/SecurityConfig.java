package in.project.computers.config;

import in.project.computers.filters.JwtAuthenticationFilter;
import in.project.computers.service.userAuthenticationService.AppUserDetailsService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // สำคัญมาก: ต้องเปิดใช้งาน @EnableMethodSecurity เพื่อให้ @PreAuthorize ทำงาน
@AllArgsConstructor
public class SecurityConfig {
    private final AppUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // ***************************************************************
                        // *** THIS IS THE FIX: Allow Docker health checks to pass ***
                        .requestMatchers("/actuator/**").permitAll()
                        // ***************************************************************

                        // --- 1. Public Endpoints (Anyone can access) ---
                        .requestMatchers("/api/register", "/api/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/components/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/orders/capture/**").permitAll() // PayPal Success Callback
                        .requestMatchers(HttpMethod.GET, "/api/orders/cancel/**").permitAll()  // PayPal Cancel Callback

                        // --- 2. Admin-Only Endpoints ---
                        .requestMatchers("/api/admin/orders/**").hasRole("ADMIN") // *** เพิ่มสำหรับ Admin Order Controller ***
                        .requestMatchers(HttpMethod.POST, "/api/components/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/components/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/components/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/components/**").hasRole("ADMIN")

                        // --- 3. Authenticated User Endpoints ---
                        .requestMatchers("/api/orders/**").authenticated() // User Order Controller (ต้องอยู่หลัง Admin และ Public)
                        .requestMatchers("/api/builds/**").authenticated()

                        // --- 4. Default Rule (Catch-all) ---
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // --- Beans อื่นๆ เหมือนเดิม ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // NOTE: For Docker, you might need to allow the frontend service name or gateway IP
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }
}