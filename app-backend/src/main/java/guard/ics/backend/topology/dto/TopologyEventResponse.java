package guard.ics.backend.topology.dto;

import guard.ics.backend.topology.entity.TopologyEventEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Network topology event")
public record TopologyEventResponse(
        @Schema(description = "Event ID") Long id,
        @Schema(description = "Global unique trace ID") String traceId,
        @Schema(description = "Event type: device_online | device_offline | topology_change") String eventType,
        @Schema(description = "Device identifier") String deviceId,
        @Schema(description = "Human-readable device name") String deviceName,
        @Schema(description = "Device type (PLC/RTU/HMI/...)") String deviceType,
        @Schema(description = "IP address") String ipAddress,
        @Schema(description = "MAC address") String macAddress,
        @Schema(description = "Network port") String port,
        @Schema(description = "Device status") String status,
        @Schema(description = "Event occurrence time") Instant occurredAt,
        @Schema(description = "Record creation time") Instant createdAt
) {
    public static TopologyEventResponse from(TopologyEventEntity e) {
        return new TopologyEventResponse(e.getId(), e.getTraceId(), e.getEventType(), e.getDeviceId(),
                e.getDeviceName(), e.getDeviceType(), e.getIpAddress(), e.getMacAddress(), e.getPort(),
                e.getStatus(), e.getOccurredAt(), e.getCreatedAt());
    }
}
