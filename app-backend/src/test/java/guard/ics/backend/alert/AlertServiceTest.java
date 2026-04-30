package guard.ics.backend.alert;

import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.alert.service.AlertService;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository);
    }

    @Test
    void shouldListAlertsWithPagination() {
        AlertEntity alert = createAlert("trace-1", "new", "medium");
        Page<AlertEntity> page = new PageImpl<>(List.of(alert), PageRequest.of(0, 20), 1);
        when(alertRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        PageDTO<?> result = alertService.list("medium", "new", null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void shouldGetAlertById() {
        AlertEntity alert = createAlert("trace-2", "new", "high");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));

        var result = alertService.getById(1L);

        assertThat(result.traceId()).isEqualTo("trace-2");
        assertThat(result.severity()).isEqualTo("high");
    }

    @Test
    void shouldThrowNotFoundWhenAlertMissing() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldTransitionStatus() {
        AlertEntity alert = createAlert("trace-3", "new", "low");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenReturn(alert);

        var result = alertService.transitionStatus(1L, "acknowledged");

        assertThat(result.status()).isEqualTo("acknowledged");
    }

    @Test
    void shouldRejectInvalidStatus() {
        assertThatThrownBy(() -> alertService.transitionStatus(1L, "invalid_status"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldEscalateAlert() {
        AlertEntity alert = createAlert("trace-5", "new", "medium");
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = alertService.escalate(1L);

        assertThat(result.severity()).isEqualTo("critical");
        assertThat(result.status()).isEqualTo("escalated");
    }

    @Test
    void shouldReturnStats() {
        when(alertRepository.countBySeveritySince(any())).thenReturn(List.of(new Object[]{"high", 3L}, new Object[]{"low", 1L}));
        when(alertRepository.countByStatusSince(any())).thenReturn(List.of(new Object[]{"new", 2L}, new Object[]{"resolved", 2L}));

        AlertStatsResponse stats = alertService.stats(null);

        assertThat(stats.bySeverity()).containsEntry("high", 3L);
        assertThat(stats.byStatus()).containsEntry("new", 2L);
        assertThat(stats.total()).isEqualTo(4L);
    }

    private AlertEntity createAlert(String traceId, String status, String severity) {
        AlertEntity alert = new AlertEntity();
        alert.setId(1L);
        alert.setTraceId(traceId);
        alert.setAlertType("anomaly");
        alert.setStatus(status);
        alert.setSeverity(severity);
        alert.setSourceIp("10.0.0.1");
        alert.setDestIp("10.0.0.2");
        alert.setProtocol("TCP");
        alert.setTriggeredAt(Instant.now());
        alert.setCreatedAt(Instant.now());
        return alert;
    }
}
