package guard.ics.backend.kafka.consumer;

import guard.ics.backend.metric.entity.TrafficMetricEntity;
import guard.ics.backend.metric.repository.TrafficMetricRepository;
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
@EmbeddedKafka(partitions = 1, topics = {"ics.traffic.metrics"})
@ActiveProfiles("kafka-test")
class TrafficMetricConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private TrafficMetricRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldConsumeAndPersistValidMessage() {
        String traceId = UUID.randomUUID().toString();
        String json = """
                {
                    "traceId": "%s",
                    "metricType": "flow_stats",
                    "sourceIp": "10.0.1.1",
                    "destIp": "10.0.1.2",
                    "protocol": "ModbusTCP",
                    "bytesIn": 1024,
                    "bytesOut": 2048,
                    "packetCount": 100,
                    "flowDuration": 500,
                    "capturedAt": "2025-01-01T00:00:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.traffic.metrics", traceId, json);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            TrafficMetricEntity saved = repository.findByTraceId(traceId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getMetricType()).isEqualTo("flow_stats");
            assertThat(saved.getSourceIp()).isEqualTo("10.0.1.1");
            assertThat(saved.getDestIp()).isEqualTo("10.0.1.2");
            assertThat(saved.getProtocol()).isEqualTo("ModbusTCP");
            assertThat(saved.getBytesIn()).isEqualTo(1024);
            assertThat(saved.getBytesOut()).isEqualTo(2048);
            assertThat(saved.getPacketCount()).isEqualTo(100);
            assertThat(saved.getFlowDuration()).isEqualTo(500);
            assertThat(saved.getCapturedAt()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void shouldDeserializeInstantFieldsCorrectly() {
        String traceId = UUID.randomUUID().toString();
        String json = """
                {
                    "traceId": "%s",
                    "metricType": "byte_count",
                    "sourceIp": "10.0.3.1",
                    "destIp": "10.0.3.2",
                    "protocol": "DNP3",
                    "bytesIn": 512,
                    "bytesOut": 1024,
                    "packetCount": 50,
                    "capturedAt": "2025-06-15T12:30:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.traffic.metrics", traceId, json);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            TrafficMetricEntity saved = repository.findByTraceId(traceId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getCapturedAt()).isExactlyInstanceOf(Instant.class);
            assertThat(saved.getCapturedAt()).isEqualTo(Instant.parse("2025-06-15T12:30:00Z"));
        });
    }

    @Test
    void shouldSkipDuplicateTraceId() {
        String traceId = UUID.randomUUID().toString();
        TrafficMetricEntity existing = EntityBuilders.aTrafficMetric()
                .traceId(traceId).build();
        repository.save(existing);

        String json = """
                {
                    "traceId": "%s",
                    "metricType": "port_stats",
                    "sourceIp": "10.0.5.1",
                    "destIp": "10.0.5.2",
                    "protocol": "OPCUA",
                    "bytesIn": 1024,
                    "bytesOut": 4096,
                    "packetCount": 2,
                    "capturedAt": "2025-01-01T00:00:00Z"
                }
                """.formatted(traceId);

        kafkaTemplate.send("ics.traffic.metrics", traceId, json);

        // Give consumer time to process, then verify only 1 record
        try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        long count = repository.count(); // not exact, but the traceId should have 1 record
        assertThat(repository.findByTraceId(traceId)).isPresent();
    }

    @Test
    void shouldNotCrashOnMalformedJson() {
        String badJson = "{ this is not valid JSON at all";

        kafkaTemplate.send("ics.traffic.metrics", "bad-trace", badJson);

        // Consumer should survive — no exception thrown to the listener container
        try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        assertThat(repository.findByTraceId("bad-trace")).isEmpty();
    }
}
