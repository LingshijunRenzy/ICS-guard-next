package guard.ics.backend.metric.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Traffic aggregation statistics")
public record TrafficStatsResponse(
        @Schema(description = "Top source IPs by total bytes") List<TopEntry> topSources,
        @Schema(description = "Top destination IPs by total bytes") List<TopEntry> topDestinations,
        @Schema(description = "Breakdown by protocol") List<ProtocolAggregate> byProtocol
) {
    @Schema(description = "Top-N entry")
    public record TopEntry(
            @Schema(description = "IP address") String ip,
            @Schema(description = "Total bytes (in + out)") long totalBytes
    ) {}
    @Schema(description = "Protocol aggregate")
    public record ProtocolAggregate(
            @Schema(description = "Protocol name") String protocol,
            @Schema(description = "Total bytes") long totalBytes,
            @Schema(description = "Total flow count") long flows
    ) {}
}
