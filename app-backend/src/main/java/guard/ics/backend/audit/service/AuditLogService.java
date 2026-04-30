package guard.ics.backend.audit.service;

import guard.ics.backend.audit.dto.AuditLogResponse;
import guard.ics.backend.audit.entity.AuditLogEntity;
import guard.ics.backend.audit.repository.AuditLogRepository;
import guard.ics.backend.common.dto.PageDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PageDTO<AuditLogResponse> list(String action, String username, String resource,
                                           Instant from, Instant to, Pageable pageable) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (username != null) predicates.add(cb.equal(root.get("username"), username));
            if (resource != null) predicates.add(cb.equal(root.get("resource"), resource));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<AuditLogEntity> page = auditLogRepository.findAll(spec, pageable);
        return PageDTO.from(page.map(AuditLogResponse::from));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new guard.ics.backend.common.exception.ResourceNotFoundException("AuditLog", id));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldLogs() {
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        int deleted = auditLogRepository.purgeOlderThan(cutoff);
        if (deleted > 0) {
            // log cleanup
        }
    }
}
