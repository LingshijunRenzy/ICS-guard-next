package guard.ics.backend.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guard.ics.backend.audit.entity.AuditLogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuditLogProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogProducer.class);
    public static final String TOPIC = "ics.audit.logs";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void send(AuditLogEntity auditLog) {
        try {
            String payload = objectMapper.writeValueAsString(auditLog);
            kafkaTemplate.send(TOPIC, auditLog.getTraceId(), payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send audit log: traceId={}", auditLog.getTraceId(), ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log", e);
        }
    }
}
