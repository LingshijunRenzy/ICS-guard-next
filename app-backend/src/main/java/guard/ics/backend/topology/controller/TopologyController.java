package guard.ics.backend.topology.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.topology.dto.TopologyEventResponse;
import guard.ics.backend.topology.service.TopologyService;
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
import java.util.List;

@RestController
@RequestMapping("/api/topology")
@Tag(name = "Topology", description = "Network topology events and device status")
public class TopologyController {

    private final TopologyService topologyService;

    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @Operation(summary = "List topology events with filters")
    @GetMapping("/events")
    @PreAuthorize("hasAuthority('topology:read')")
    public ApiResponse<PageDTO<TopologyEventResponse>> listEvents(
            @Parameter(description = "Event type") @RequestParam(required = false) String eventType,
            @Parameter(description = "Device ID") @RequestParam(required = false) String deviceId,
            @Parameter(description = "Status") @RequestParam(required = false) String status,
            @Parameter(description = "Start time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(topologyService.listEvents(eventType, deviceId, status, from, to, pageable));
    }

    @Operation(summary = "Get topology event by ID")
    @GetMapping("/events/{id}")
    @PreAuthorize("hasAuthority('topology:read')")
    public ApiResponse<TopologyEventResponse> getEvent(@Parameter(description = "Event ID") @PathVariable Long id) {
        return ApiResponse.success(topologyService.getEventById(id));
    }

    @Operation(summary = "List devices with latest status")
    @GetMapping("/devices")
    @PreAuthorize("hasAuthority('topology:read')")
    public ApiResponse<List<TopologyEventResponse>> getDevices() {
        return ApiResponse.success(topologyService.getLatestDevices());
    }

    @Operation(summary = "Get event history for a device")
    @GetMapping("/devices/{deviceId}/history")
    @PreAuthorize("hasAuthority('topology:read')")
    public ApiResponse<List<TopologyEventResponse>> getDeviceHistory(@Parameter(description = "Device ID") @PathVariable String deviceId) {
        return ApiResponse.success(topologyService.getDeviceHistory(deviceId));
    }
}
