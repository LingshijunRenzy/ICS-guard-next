package guard.ics.backend.metric.dto;

import guard.ics.backend.metric.entity.TrafficMetricEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Aggregated traffic flow metric")
public record TrafficMetricResponse(
        @Schema(description = "Metric ID") Long id,
        @Schema(description = "Global unique trace ID") String traceId,
        @Schema(description = "Metric type") String metricType,
        @Schema(description = "Source IP address") String sourceIp,
        @Schema(description = "Destination IP address") String destIp,
        @Schema(description = "Protocol") String protocol,
        @Schema(description = "Bytes received") long bytesIn,
        @Schema(description = "Bytes sent") long bytesOut,
        @Schema(description = "Total packet count") long packetCount,
        @Schema(description = "Flow duration in milliseconds") Long flowDuration,
        @Schema(description = "Metric capture time") Instant capturedAt,
        @Schema(description = "Record creation time") Instant createdAt
) {
    public static TrafficMetricResponse from(TrafficMetricEntity e) {
        return new TrafficMetricResponse(e.getId(), e.getTraceId(), e.getMetricType(),
                e.getSourceIp(), e.getDestIp(), e.getProtocol(), e.getBytesIn(), e.getBytesOut(),
                e.getPacketCount(), e.getFlowDuration(), e.getCapturedAt(), e.getCreatedAt());
    }
}
