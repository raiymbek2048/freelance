package kg.freelance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "freedompay")
@Getter
@Setter
public class FreedomPayConfig {
    private String merchantId;
    private String secretKey;
    private String apiUrl = "https://api.freedompay.kg";
    private boolean testMode = true;
}
