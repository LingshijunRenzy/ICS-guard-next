package guard.ics.backend.audit;

import guard.ics.backend.audit.dto.AuditLogResponse;
import guard.ics.backend.audit.entity.AuditLogEntity;
import guard.ics.backend.audit.repository.AuditLogRepository;
import guard.ics.backend.audit.service.AuditLogService;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository);
    }

    // ── list ──────────────────────────────────────────────────────

    @Test
    void shouldListAuditLogsWithAllFilters() {
        AuditLogEntity entity = createEntity(1L, "login", "admin", "auth");
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<AuditLogResponse> result = auditLogService.list(
                "login", "admin", "auth", Instant.now().minus(1, ChronoUnit.HOURS), Instant.now(), org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).action()).isEqualTo("login");
        assertThat(result.content().get(0).username()).isEqualTo("admin");
        assertThat(result.content().get(0).resource()).isEqualTo("auth");
    }

    @Test
    void shouldListAuditLogsWithoutFilters() {
        AuditLogEntity e1 = createEntity(1L, "login", "admin", "auth");
        AuditLogEntity e2 = createEntity(2L, "create_rule", "operator", "rule");
        Page<AuditLogEntity> page = new PageImpl<>(List.of(e1, e2));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<AuditLogResponse> result = auditLogService.list(
                null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    @Test
    void shouldReturnEmptyAuditLogPage() {
        Page<AuditLogEntity> empty = Page.empty();
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        PageDTO<AuditLogResponse> result = auditLogService.list(
                null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void shouldListAuditLogsWithPartialFilters() {
        AuditLogEntity entity = createEntity(1L, "delete_rule", "admin", "rule");
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<AuditLogResponse> result = auditLogService.list(
                "delete_rule", null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).action()).isEqualTo("delete_rule");
    }

    // ── getById ───────────────────────────────────────────────────

    @Test
    void shouldGetAuditLogById() {
        AuditLogEntity entity = createEntity(1L, "logout", "viewer", "auth");
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(entity));

        AuditLogResponse result = auditLogService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.traceId()).isEqualTo("trace-1");
        assertThat(result.action()).isEqualTo("logout");
        assertThat(result.username()).isEqualTo("viewer");
        assertThat(result.resource()).isEqualTo("auth");
        assertThat(result.ipAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void shouldThrowNotFoundWhenAuditLogMissing() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditLogService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── purgeOldLogs ──────────────────────────────────────────────

    @Test
    void shouldPurgeOldLogsAndReturnDeletedCount() {
        when(auditLogRepository.purgeOlderThan(any(Instant.class))).thenReturn(42);

        auditLogService.purgeOldLogs();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(auditLogRepository).purgeOlderThan(cutoffCaptor.capture());
        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedCutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        assertThat(cutoff).isCloseTo(expectedCutoff, within(5, ChronoUnit.SECONDS));
    }

    @Test
    void shouldPurgeOldLogsWhenNothingToDelete() {
        when(auditLogRepository.purgeOlderThan(any(Instant.class))).thenReturn(0);

        auditLogService.purgeOldLogs();

        verify(auditLogRepository).purgeOlderThan(any(Instant.class));
    }

    // ── helpers ───────────────────────────────────────────────────

    private AuditLogEntity createEntity(Long id, String action, String username, String resource) {
        return AuditLogEntity.builder()
                .id(id).traceId("trace-" + id).userId(1L).username(username)
                .action(action).resource(resource).resourceId("res-" + id)
                .ipAddress("10.0.0.1")
                .createdAt(Instant.now()).build();
    }
}
