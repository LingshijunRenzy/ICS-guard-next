package guard.ics.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    public AlertConsumer(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "ics.threat.alerts", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void consume(String message) {
        try {
            AlertEntity alert = objectMapper.readValue(message, AlertEntity.class);
            if (alertRepository.findByTraceId(alert.getTraceId()).isPresent()) {
                log.debug("Duplicate alert skipped: traceId={}", alert.getTraceId());
                return;
            }
            alert.setId(null);
            alertRepository.save(alert);
            log.info("Alert saved: traceId={}, type={}, severity={}", alert.getTraceId(), alert.getAlertType(), alert.getSeverity());
        } catch (Exception e) {
            log.error("Failed to consume alert message", e);
        }
    }
}
