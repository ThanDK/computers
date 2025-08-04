package in.project.computers.config;

import com.paypal.base.rest.APIContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// No need for these imports anymore as the logic is encapsulated
// import com.paypal.base.rest.OAuthTokenCredential;
// import java.util.HashMap;
// import java.util.Map;

@Configuration
public class PaypalConfig {

    @Value("${paypal.client.id}")
    private String clientId;

    @Value("${paypal.client.secret}")
    private String clientSecret;

    @Value("${paypal.mode}")
    private String mode;

    /*
     * The following two beans are no longer needed to create the APIContext.
     * The new APIContext constructor handles this setup internally.
     * You can remove them unless they are being used by other beans in your application.
     */
    /*
    @Bean
    public Map<String, String> paypalSdkConfig() {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("mode", mode);
        return configMap;
    }

    @Bean
    public OAuthTokenCredential oAuthTokenCredential() {
        return new OAuthTokenCredential(clientId, clientSecret, paypalSdkConfig());
    }
    */


    /**
     * This is the updated bean definition for APIContext.
     * It uses the non-deprecated constructor that takes client ID, secret, and mode directly.
     * This allows the APIContext object to manage the OAuth token itself,
     *
     * @return A configured APIContext object.
     //* @throws PayPalRESTException
     */
    @Bean
    public APIContext apiContext() {
        return new APIContext(clientId, clientSecret, mode);
    }
}