package guard.ics.backend.alert.controller;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.service.AlertService;
import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.dto.PageDTO;
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
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Threat alert management")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "List alerts with filters")
    @GetMapping
    @PreAuthorize("hasAuthority('alerts:read')")
    public ApiResponse<PageDTO<AlertResponse>> list(
            @Parameter(description = "Severity: low, medium, high, critical") @RequestParam(required = false) String severity,
            @Parameter(description = "Status: new, acknowledged, resolved, escalated, false_positive") @RequestParam(required = false) String status,
            @Parameter(description = "Alert type") @RequestParam(required = false) String alertType,
            @Parameter(description = "Source IP address") @RequestParam(required = false) String sourceIp,
            @Parameter(description = "Destination IP address") @RequestParam(required = false) String destIp,
            @Parameter(description = "Start time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(alertService.list(severity, status, alertType, sourceIp, destIp, from, to, pageable));
    }

    @Operation(summary = "Get alert by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('alerts:read')")
    public ApiResponse<AlertResponse> get(@Parameter(description = "Alert ID") @PathVariable Long id) {
        return ApiResponse.success(alertService.getById(id));
    }

    @Operation(summary = "Transition alert status")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('alerts:manage')")
    public ApiResponse<AlertResponse> transitionStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.success(alertService.transitionStatus(id, body.get("status")));
    }

    @Operation(summary = "Escalate alert to critical")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('alerts:manage')")
    public ApiResponse<AlertResponse> escalate(@PathVariable Long id) {
        return ApiResponse.success(alertService.escalate(id));
    }

    @Operation(summary = "Get alert statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('alerts:read')")
    public ApiResponse<AlertStatsResponse> stats(
            @Parameter(description = "Start time (ISO 8601), defaults to 24h ago") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        return ApiResponse.success(alertService.stats(since));
    }
}
