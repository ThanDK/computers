package in.project.computers.config;

import in.project.computers.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    // URL ของฝั่ง Frontend สำหรับ redirect กลับไป
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        log.info("OAuth2 login successful. Entering custom success handler.");

        try {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

            // สร้าง JWT ของระบบเราเอง โดยใช้ข้อมูล user จาก OAuth2
            final String jwtToken = jwtUtil.generateToken(authentication);
            log.info("Successfully generated JWT for user: {}", oidcUser.getEmail());

            // เตรียม URL สำหรับ redirect กลับไปที่หน้า frontend พร้อมแนบ token ไปใน query string
            String redirectUrl = frontendUrl + "/login-success?token=" + jwtToken;
            log.info("Redirecting to: {}", redirectUrl);

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("Error occurred in OAuth2LoginSuccessHandler", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing OAuth2 login.");
        }
    }
}