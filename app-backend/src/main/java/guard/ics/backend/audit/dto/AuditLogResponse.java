package guard.ics.backend.audit.dto;

import guard.ics.backend.audit.entity.AuditLogEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Audit log entry")
public record AuditLogResponse(
        @Schema(description = "Audit log ID") Long id,
        @Schema(description = "Global unique trace ID") String traceId,
        @Schema(description = "User ID who performed the action") Long userId,
        @Schema(description = "Username who performed the action") String username,
        @Schema(description = "Action type (login, logout, create_rule, ...)") String action,
        @Schema(description = "Target resource type") String resource,
        @Schema(description = "Target resource ID") String resourceId,
        @Schema(description = "Client IP address") String ipAddress,
        @Schema(description = "Event time") Instant createdAt
) {
    public static AuditLogResponse from(AuditLogEntity e) {
        return new AuditLogResponse(e.getId(), e.getTraceId(), e.getUserId(), e.getUsername(),
                e.getAction(), e.getResource(), e.getResourceId(), e.getIpAddress(), e.getCreatedAt());
    }
}
