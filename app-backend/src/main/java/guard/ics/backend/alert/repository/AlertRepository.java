package guard.ics.backend.alert.repository;

import guard.ics.backend.alert.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<AlertEntity, Long>, JpaSpecificationExecutor<AlertEntity> {

    Optional<AlertEntity> findByTraceId(String traceId);

    @Query("SELECT a.severity, COUNT(a) FROM AlertEntity a WHERE a.triggeredAt >= :since GROUP BY a.severity")
    List<Object[]> countBySeveritySince(Instant since);

    @Query("SELECT a.status, COUNT(a) FROM AlertEntity a WHERE a.triggeredAt >= :since GROUP BY a.status")
    List<Object[]> countByStatusSince(Instant since);

    @Query("SELECT a FROM AlertEntity a WHERE a.status = 'new' AND a.severity = 'critical' ORDER BY a.triggeredAt DESC")
    List<AlertEntity> findCriticalNew();

    List<AlertEntity> findTop10ByOrderByTriggeredAtDesc();
}
