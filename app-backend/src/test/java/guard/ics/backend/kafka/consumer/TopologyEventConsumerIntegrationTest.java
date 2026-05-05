package guard.ics.backend.kafka.consumer;

import guard.ics.backend.topology.entity.TopologyEventEntity;
import guard.ics.backend.topology.repository.TopologyEventRepository;
import guard.ics.backend.support.EntityBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"ics.topology.events"})
@ActiveProfiles("kafka-test")
class TopologyEventConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TopologyEventRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldConsumeAndPersistValidEvent() {
        String traceId = UUID.randomUUID().toString();
        String json = """
                {
                    "traceId": "%s",
                    "eventType": "device_online",
                    "deviceId": "plc-unit1",
                    "deviceName": "PLC Unit 1",
                    "deviceType": "plc",
                    "ipAddress": "10.0.2.1",
                    "macAddress": "00:11:22:33:44:55",
                    "port": "502",
                    "status": "online",
                    "metadata": "{\\"zone\\":\\"unit1\\",\\"changeReason\\":\\"device_discovered\\"}",
                    "occurredAt": "2025-01-01T00:00:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.topology.events", traceId, json);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            TopologyEventEntity saved = repository.findByTraceId(traceId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getEventType()).isEqualTo("device_online");
            assertThat(saved.getDeviceId()).isEqualTo("plc-unit1");
            assertThat(saved.getDeviceName()).isEqualTo("PLC Unit 1");
            assertThat(saved.getIpAddress()).isEqualTo("10.0.2.1");
            assertThat(saved.getMacAddress()).isEqualTo("00:11:22:33:44:55");
            assertThat(saved.getPort()).isEqualTo("502");
            assertThat(saved.getStatus()).isEqualTo("online");
            assertThat(saved.getOccurredAt()).isExactlyInstanceOf(Instant.class);
        });
    }

    @Test
    void shouldPersistMetadataAsJson() {
        String traceId = UUID.randomUUID().toString();
        String metadataJson = "{\"zone\":\"switchyard\",\"changeReason\":\"link_up\"}";
        String json = """
                {
                    "traceId": "%s",
                    "eventType": "topology_change",
                    "deviceId": "rtu-sy1",
                    "deviceName": "RTU Switchyard 1",
                    "deviceType": "rtu",
                    "ipAddress": "10.0.3.1",
                    "macAddress": "aa:bb:cc:dd:ee:ff",
                    "port": "20000",
                    "status": "online",
                    "metadata": "%s",
                    "occurredAt": "2025-03-15T08:30:00Z"
                }
                """.formatted(traceId, metadataJson.replace("\"", "\\\""));

        kafkaTemplate.send("ics.topology.events", traceId, json);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            TopologyEventEntity saved = repository.findByTraceId(traceId).orElse(null);
            assertThat(saved).isNotNull();
            // The metadata String should contain the original JSON
            assertThat(saved.getMetadata()).contains("switchyard", "link_up");
        });
    }

    @Test
    void shouldSkipDuplicateTraceId() {
        String traceId = UUID.randomUUID().toString();
        TopologyEventEntity existing = EntityBuilders.aTopologyEvent()
                .traceId(traceId).build();
        repository.save(existing);

        String json = """
                {
                    "traceId": "%s",
                    "eventType": "device_offline",
                    "deviceId": "plc-unit2",
                    "deviceName": "PLC Unit 2",
                    "deviceType": "plc",
                    "ipAddress": "10.0.2.2",
                    "macAddress": "11:22:33:44:55:66",
                    "port": "502",
                    "status": "offline",
                    "metadata": "{}",
                    "occurredAt": "2025-01-01T00:00:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.topology.events", traceId, json);

        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThat(repository.findByTraceId(traceId)).isPresent();
    }

    @Test
    void shouldNotCrashOnMalformedJson() {
        kafkaTemplate.send("ics.topology.events", "bad-trace", "{not valid}");
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        assertThat(repository.findByTraceId("bad-trace")).isEmpty();
    }
}
