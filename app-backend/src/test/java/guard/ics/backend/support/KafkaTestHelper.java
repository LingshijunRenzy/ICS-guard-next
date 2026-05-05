package guard.ics.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.*;

/** Helpers for embedded Kafka integration tests. */
public final class KafkaTestHelper {

    public static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    /** Consume a single record from a topic. Returns null if no record within timeout. */
    public static ConsumerRecord<String, String> consumeOne(
            String brokers, String topic, long timeoutMs) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                brokers, "test-consumer-" + System.currentTimeMillis(), "true");
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (var consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                .createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(timeoutMs));
            if (records.isEmpty()) return null;
            for (ConsumerRecord<String, String> r : records) {
                return r;
            }
            return null;
        }
    }

    /** Consume all records from a topic within timeout. */
    public static List<ConsumerRecord<String, String>> consumeAll(
            String brokers, String topic, long timeoutMs) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                brokers, "test-consumer-" + System.currentTimeMillis(), "true");
        consumerProps.put(KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(VALUE_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (var consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps)
                .createConsumer()) {
            consumer.subscribe(Collections.singletonList(topic));
            var records = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(timeoutMs));
            List<ConsumerRecord<String, String>> result = new ArrayList<>();
            records.forEach(result::add);
            return result;
        }
    }

    /** Create a test producer template */
    public static KafkaTemplate<String, String> createTemplate(String brokers) {
        Map<String, Object> props = KafkaTestUtils.producerProps(brokers);
        props.put(KEY_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
