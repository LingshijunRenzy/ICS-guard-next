package guard.ics.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.alert.service.AlertWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final AlertWebSocketService alertWebSocketService;

    public AlertConsumer(AlertRepository alertRepository, AlertWebSocketService alertWebSocketService) {
        this.alertRepository = alertRepository;
        this.objectMapper = new ObjectMapper();
        this.alertWebSocketService = alertWebSocketService;
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
            AlertEntity saved = alertRepository.save(alert);
            alertWebSocketService.broadcastNewAlert(AlertResponse.from(saved));
            log.info("Alert saved: traceId={}, type={}, severity={}", saved.getTraceId(), saved.getAlertType(), saved.getSeverity());
        } catch (Exception e) {
            log.error("Failed to consume alert message", e);
        }
    }
}
