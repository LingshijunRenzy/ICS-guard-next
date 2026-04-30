package guard.ics.backend.dashboard.dto;

import guard.ics.backend.alert.dto.AlertResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Unified dashboard overview")
public record DashboardResponse(
        @Schema(description = "Total alerts in period") long totalAlerts,
        @Schema(description = "Alert count by severity") Map<String, Long> alertsBySeverity,
        @Schema(description = "Alert count by status") Map<String, Long> alertsByStatus,
        @Schema(description = "Number of unique devices in topology") long totalDevices,
        @Schema(description = "Active (enabled) rule count") long activeRuleCount,
        @Schema(description = "Total rule count") long totalRuleCount,
        @Schema(description = "Most recent 10 alerts") List<AlertResponse> recentAlerts,
        @Schema(description = "Traffic summary (top sources & destinations)") TrafficSummary trafficSummary
) {
    @Schema(description = "Traffic overview")
    public record TrafficSummary(
            @Schema(description = "Top source IPs by bytes") List<TopEntry> topSources,
            @Schema(description = "Top destination IPs by bytes") List<TopEntry> topDestinations
    ) {}

    @Schema(description = "Traffic top entry")
    public record TopEntry(
            @Schema(description = "IP address") String ip,
            @Schema(description = "Total bytes") long totalBytes
    ) {}
}
