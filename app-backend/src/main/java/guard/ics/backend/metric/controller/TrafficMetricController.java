package guard.ics.backend.metric.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.metric.dto.TrafficMetricResponse;
import guard.ics.backend.metric.dto.TrafficStatsResponse;
import guard.ics.backend.metric.service.TrafficMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/metrics/traffic")
@Tag(name = "Traffic Metrics", description = "Aggregated traffic flow metrics")
public class TrafficMetricController {

    private final TrafficMetricService trafficMetricService;

    public TrafficMetricController(TrafficMetricService trafficMetricService) {
        this.trafficMetricService = trafficMetricService;
    }

    @Operation(summary = "List traffic metrics with filters")
    @GetMapping
    @PreAuthorize("hasAuthority('metrics:read')")
    public ApiResponse<PageDTO<TrafficMetricResponse>> list(
            @Parameter(description = "Source IP address") @RequestParam(required = false) String sourceIp,
            @Parameter(description = "Destination IP address") @RequestParam(required = false) String destIp,
            @Parameter(description = "Protocol: tcp, udp, icmp, modbus, dnp3, etc.") @RequestParam(required = false) String protocol,
            @Parameter(description = "Start time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(trafficMetricService.list(sourceIp, destIp, protocol, from, to, pageable));
    }

    @Operation(summary = "Get traffic metric by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('metrics:read')")
    public ApiResponse<TrafficMetricResponse> get(@Parameter(description = "Metric ID") @PathVariable Long id) {
        return ApiResponse.success(trafficMetricService.getById(id));
    }

    @Operation(summary = "Get traffic statistics (Top-N sources, destinations, protocol breakdown)")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('metrics:read')")
    public ApiResponse<TrafficStatsResponse> stats(
            @Parameter(description = "Start time (ISO 8601), defaults to 1h ago") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ApiResponse.success(trafficMetricService.stats(since));
    }
}
