package guard.ics.backend.audit.repository;

import guard.ics.backend.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.createdAt < :before")
    int purgeOlderThan(Instant before);
}
