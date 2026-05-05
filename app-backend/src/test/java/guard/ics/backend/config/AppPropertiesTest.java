package guard.ics.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableConfigurationProperties(AppProperties.class)
class AppPropertiesTest {

    @Autowired private AppProperties appProperties;

    @Test
    void shouldBindControllerBaseUrl() {
        assertThat(appProperties.controller().baseUrl()).isEqualTo("http://localhost:8000");
    }

    @Test
    void shouldBindErrorIncludeDetails() {
        assertThat(appProperties.error().includeDetails()).isFalse();
    }
}
