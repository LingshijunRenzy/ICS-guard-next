package guard.ics.backend.alert.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Alert aggregation statistics")
public record AlertStatsResponse(
        @Schema(description = "Alert count by severity") Map<String, Long> bySeverity,
        @Schema(description = "Alert count by status") Map<String, Long> byStatus,
        @Schema(description = "Total alert count in the period") long total
) {}
