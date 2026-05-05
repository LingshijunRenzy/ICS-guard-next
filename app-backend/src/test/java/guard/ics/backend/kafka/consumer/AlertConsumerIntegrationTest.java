package guard.ics.backend.kafka.consumer;

import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.alert.service.AlertWebSocketService;
import guard.ics.backend.support.EntityBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"ics.threat.alerts"})
@ActiveProfiles("kafka-test")
class AlertConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private AlertRepository repository;

    @MockitoBean
    private AlertWebSocketService alertWebSocketService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldConsumeAndPersistValidAlert() {
        String traceId = UUID.randomUUID().toString();
        String json = """
                {
                    "traceId": "%s",
                    "alertType": "anomaly_traffic",
                    "severity": "high",
                    "sourceIp": "10.0.0.1",
                    "destIp": "10.0.0.2",
                    "protocol": "ModbusTCP",
                    "description": "Abnormal Modbus traffic burst from PLC-001",
                    "rawPayload": "{\\"confidence\\":0.95,\\"engine\\":\\"mock\\"}",
                    "status": "new",
                    "triggeredAt": "2025-06-15T12:30:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.threat.alerts", traceId, json);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            AlertEntity saved = repository.findByTraceId(traceId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getAlertType()).isEqualTo("anomaly_traffic");
            assertThat(saved.getSeverity()).isEqualTo("high");
            assertThat(saved.getSourceIp()).isEqualTo("10.0.0.1");
            assertThat(saved.getDestIp()).isEqualTo("10.0.0.2");
            assertThat(saved.getProtocol()).isEqualTo("ModbusTCP");
            assertThat(saved.getDescription()).isEqualTo("Abnormal Modbus traffic burst from PLC-001");
            assertThat(saved.getStatus()).isEqualTo("new");
            assertThat(saved.getTriggeredAt()).isExactlyInstanceOf(Instant.class);
            assertThat(saved.getRawPayload()).contains("confidence");
        });

        verify(alertWebSocketService).broadcastNewAlert(any());
    }

    @Test
    void shouldSkipDuplicateAlert() {
        String traceId = UUID.randomUUID().toString();
        AlertEntity existing = EntityBuilders.anAlert()
                .traceId(traceId).build();
        repository.save(existing);

        String json = """
                {
                    "traceId": "%s",
                    "alertType": "denial_of_service",
                    "severity": "critical",
                    "sourceIp": "192.168.1.1",
                    "destIp": "192.168.1.2",
                    "protocol": "TCP",
                    "description": "Duplicate should be skipped",
                    "rawPayload": "{}",
                    "status": "new",
                    "triggeredAt": "2025-01-01T00:00:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.threat.alerts", traceId, json);

        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThat(repository.findByTraceId(traceId)).isPresent();
        // Should not have called broadcast for the duplicate
    }

    @Test
    void shouldNotCrashOnMalformedJson() {
        kafkaTemplate.send("ics.threat.alerts", "bad-trace", "{corrupt}");
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(repository.findByTraceId("bad-trace")).isEmpty();
    }
}
