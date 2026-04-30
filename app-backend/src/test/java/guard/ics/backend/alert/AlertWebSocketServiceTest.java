package guard.ics.backend.alert;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.service.AlertWebSocketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertWebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private AlertWebSocketService alertWebSocketService;

    @Test
    void shouldSendNewAlertToTopic() {
        AlertResponse alert = new AlertResponse(1L, "trace-1", "anomaly", "high",
                "10.0.0.1", "10.0.0.2", "TCP", "Test alert", "new",
                Instant.now(), Instant.now());

        alertWebSocketService.broadcastNewAlert(alert);

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/new"), eq(alert));
    }

    @Test
    void shouldSendStatusChangeToTopic() {
        alertWebSocketService.broadcastStatusChange(42L, "acknowledged");

        verify(messagingTemplate).convertAndSend(eq("/topic/alerts/status"), any(Object.class));
    }
}
