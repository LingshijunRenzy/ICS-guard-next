package guard.ics.backend.metric.repository;

import guard.ics.backend.metric.entity.TrafficMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrafficMetricRepository extends JpaRepository<TrafficMetricEntity, Long>, JpaSpecificationExecutor<TrafficMetricEntity> {

    Optional<TrafficMetricEntity> findByTraceId(String traceId);

    @Query("SELECT t.sourceIp, SUM(t.bytesIn + t.bytesOut) as total FROM TrafficMetricEntity t " +
           "WHERE t.capturedAt >= :since GROUP BY t.sourceIp ORDER BY total DESC")
    List<Object[]> topSourcesByBytes(Instant since, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.destIp, SUM(t.bytesIn + t.bytesOut) as total FROM TrafficMetricEntity t " +
           "WHERE t.capturedAt >= :since GROUP BY t.destIp ORDER BY total DESC")
    List<Object[]> topDestinationsByBytes(Instant since, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.protocol, SUM(t.bytesIn + t.bytesOut) as bytes, COUNT(t) as flows FROM TrafficMetricEntity t " +
           "WHERE t.capturedAt >= :since GROUP BY t.protocol ORDER BY bytes DESC")
    List<Object[]> aggregateByProtocol(Instant since);
}
