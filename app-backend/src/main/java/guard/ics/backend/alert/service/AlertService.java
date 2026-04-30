package guard.ics.backend.alert.service;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AlertService {

    private static final Set<String> VALID_SEVERITIES = Set.of("low", "medium", "high", "critical");
    private static final Set<String> VALID_STATUSES = Set.of("new", "acknowledged", "resolved", "escalated", "false_positive");

    private final AlertRepository alertRepository;
    private final AlertWebSocketService alertWebSocketService;

    public AlertService(AlertRepository alertRepository, AlertWebSocketService alertWebSocketService) {
        this.alertRepository = alertRepository;
        this.alertWebSocketService = alertWebSocketService;
    }

    @Transactional(readOnly = true)
    public PageDTO<AlertResponse> list(String severity, String status, String alertType,
                                        String sourceIp, String destIp, Instant from, Instant to, Pageable pageable) {
        Specification<AlertEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (severity != null) predicates.add(cb.equal(root.get("severity"), severity));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (alertType != null) predicates.add(cb.equal(root.get("alertType"), alertType));
            if (sourceIp != null) predicates.add(cb.equal(root.get("sourceIp"), sourceIp));
            if (destIp != null) predicates.add(cb.equal(root.get("destIp"), destIp));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("triggeredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("triggeredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AlertEntity> page = alertRepository.findAll(spec, pageable);
        return PageDTO.from(page.map(AlertResponse::from));
    }

    @Transactional(readOnly = true)
    public AlertResponse getById(Long id) {
        return AlertResponse.from(alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id)));
    }

    @Transactional
    public AlertResponse transitionStatus(Long id, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new BadRequestException("Invalid status: " + newStatus);
        }
        AlertEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setStatus(newStatus);
        AlertResponse response = AlertResponse.from(alertRepository.save(alert));
        alertWebSocketService.broadcastStatusChange(id, newStatus);
        return response;
    }

    @Transactional
    public AlertResponse escalate(Long id) {
        AlertEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (!"new".equals(alert.getStatus()) && !"acknowledged".equals(alert.getStatus())) {
            throw new BadRequestException("Only new or acknowledged alerts can be escalated");
        }
        alert.setSeverity("critical");
        alert.setStatus("escalated");
        AlertResponse response = AlertResponse.from(alertRepository.save(alert));
        alertWebSocketService.broadcastStatusChange(id, "escalated");
        return response;
    }

    @Transactional(readOnly = true)
    public AlertStatsResponse stats(Instant since) {
        if (since == null) since = Instant.now().minusSeconds(86400);
        List<Object[]> severityRows = alertRepository.countBySeveritySince(since);
        List<Object[]> statusRows = alertRepository.countByStatusSince(since);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : severityRows) bySeverity.put((String) row[0], (Long) row[1]);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : statusRows) byStatus.put((String) row[0], (Long) row[1]);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new AlertStatsResponse(bySeverity, byStatus, total);
    }
}
