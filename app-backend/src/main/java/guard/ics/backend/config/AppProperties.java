package guard.ics.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Controller controller) {

    public record Controller(String baseUrl) {}
}
