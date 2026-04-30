package guard.ics.backend.alert.dto;

import guard.ics.backend.alert.entity.AlertEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Threat alert information")
public record AlertResponse(
        @Schema(description = "Alert ID") Long id,
        @Schema(description = "Global unique trace ID (idempotency key)") String traceId,
        @Schema(description = "Alert type / category") String alertType,
        @Schema(description = "Severity: low | medium | high | critical") String severity,
        @Schema(description = "Source IP address") String sourceIp,
        @Schema(description = "Destination IP address") String destIp,
        @Schema(description = "Protocol (TCP/UDP/ICMP/...)") String protocol,
        @Schema(description = "Human-readable description") String description,
        @Schema(description = "Processing status") String status,
        @Schema(description = "Time when the alert was triggered") Instant triggeredAt,
        @Schema(description = "Record creation time") Instant createdAt
) {
    public static AlertResponse from(AlertEntity e) {
        return new AlertResponse(e.getId(), e.getTraceId(), e.getAlertType(), e.getSeverity(),
                e.getSourceIp(), e.getDestIp(), e.getProtocol(), e.getDescription(),
                e.getStatus(), e.getTriggeredAt(), e.getCreatedAt());
    }
}
