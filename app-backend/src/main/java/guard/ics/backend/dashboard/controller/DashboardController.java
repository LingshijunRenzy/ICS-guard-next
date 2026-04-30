package guard.ics.backend.dashboard.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.dashboard.dto.DashboardResponse;
import guard.ics.backend.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Unified overview and aggregation")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Get unified dashboard overview")
    @GetMapping
    @PreAuthorize("hasAuthority('dashboard:read')")
    public ApiResponse<DashboardResponse> aggregate() {
        return ApiResponse.success(dashboardService.aggregate());
    }
}
