package kg.freelance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitConfig {

    private Auth auth = new Auth();
    private int general = 100;

    @Getter
    @Setter
    public static class Auth {
        private int login = 5;
        private int register = 3;
        private int refresh = 10;
    }
}
