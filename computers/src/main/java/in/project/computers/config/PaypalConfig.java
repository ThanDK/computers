package in.project.computers.config;

import com.paypal.base.rest.APIContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaypalConfig {

    @Value("${paypal.client.id}")
    private String clientId;
    @Value("${paypal.client.secret}")
    private String clientSecret;
    @Value("${paypal.mode}")
    private String mode; //"sandbox" or "live"

    /**
     * สร้าง Bean ของ {@link APIContext} สำหรับใช้ในการเรียก Paypal API
     */
    @Bean
    public APIContext apiContext() {
        // APIContext
        return new APIContext(clientId, clientSecret, mode);
    }
}