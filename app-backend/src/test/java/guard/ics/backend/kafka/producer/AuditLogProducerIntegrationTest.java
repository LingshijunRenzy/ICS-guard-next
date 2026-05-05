package guard.ics.backend.kafka.producer;

import guard.ics.backend.audit.entity.AuditLogEntity;
import guard.ics.backend.support.EntityBuilders;
import guard.ics.backend.support.KafkaTestHelper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"ics.audit.logs"})
@ActiveProfiles("kafka-test")
class AuditLogProducerIntegrationTest {

    @Autowired
    private AuditLogProducer producer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void shouldSendAuditLogWithTraceIdAsKey() {
        AuditLogEntity entity = EntityBuilders.anAuditLog()
                .traceId(UUID.randomUUID().toString())
                .createdAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build();

        producer.send(entity);

        List<ConsumerRecord<String, String>> records =
                KafkaTestHelper.consumeAll(embeddedKafkaBroker.getBrokersAsString(), "ics.audit.logs", 10000);
        assertThat(records).anyMatch(r ->
                entity.getTraceId().equals(r.key()) && r.value().contains("\"action\":\"USER_CREATE\""));
    }

    @Test
    void shouldSerializeInstantField() {
        AuditLogEntity entity = EntityBuilders.anAuditLog()
                .traceId(UUID.randomUUID().toString())
                .createdAt(Instant.parse("2025-06-15T12:30:00Z"))
                .build();

        producer.send(entity);

        List<ConsumerRecord<String, String>> records =
                KafkaTestHelper.consumeAll(embeddedKafkaBroker.getBrokersAsString(), "ics.audit.logs", 10000);
        assertThat(records).anyMatch(r ->
                entity.getTraceId().equals(r.key()) && r.value().contains("\"createdAt\""));
    }
}
