package guard.ics.backend.alert.service;

import guard.ics.backend.alert.dto.AlertResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlertWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(AlertWebSocketService.class);
    private final SimpMessagingTemplate messagingTemplate;

    public AlertWebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastNewAlert(AlertResponse alert) {
        messagingTemplate.convertAndSend("/topic/alerts/new", alert);
        log.debug("WebSocket broadcast: new alert traceId={}", alert.traceId());
    }

    public void broadcastStatusChange(Long alertId, String newStatus) {
        messagingTemplate.convertAndSend("/topic/alerts/status",
                (Object) Map.of("alertId", alertId, "status", newStatus));
        log.debug("WebSocket broadcast: alert {} status -> {}", alertId, newStatus);
    }
}
