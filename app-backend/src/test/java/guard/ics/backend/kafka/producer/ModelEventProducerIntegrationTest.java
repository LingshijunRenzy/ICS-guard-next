package guard.ics.backend.kafka.producer;

import guard.ics.backend.support.KafkaTestHelper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"ics.model.events"})
@ActiveProfiles("kafka-test")
class ModelEventProducerIntegrationTest {

    @Autowired
    private ModelEventProducer producer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void shouldSendModelEventWithEventTypeAsKey() {
        producer.send("model_update", "{\"modelId\":\"ml-001\",\"version\":2}");

        List<ConsumerRecord<String, String>> records =
                KafkaTestHelper.consumeAll(embeddedKafkaBroker.getBrokersAsString(), "ics.model.events", 10000);
        assertThat(records).anyMatch(r ->
                "model_update".equals(r.key()) && r.value().contains("ml-001"));
    }

    @Test
    void shouldSendPlainTextPayload() {
        producer.send("threshold_change", "new_threshold=0.85");

        List<ConsumerRecord<String, String>> records =
                KafkaTestHelper.consumeAll(embeddedKafkaBroker.getBrokersAsString(), "ics.model.events", 10000);
        assertThat(records).anyMatch(r ->
                "threshold_change".equals(r.key()) && r.value().equals("new_threshold=0.85"));
    }
}
