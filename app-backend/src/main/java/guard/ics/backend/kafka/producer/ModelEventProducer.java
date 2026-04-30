package guard.ics.backend.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ModelEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ModelEventProducer.class);
    public static final String TOPIC = "ics.model.events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ModelEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(String eventType, String payload) {
        kafkaTemplate.send(TOPIC, eventType, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send model event: type={}", eventType, ex);
            } else {
                log.info("Model event sent: type={}, offset={}", eventType,
                        result.getRecordMetadata().offset());
            }
        });
    }
}
