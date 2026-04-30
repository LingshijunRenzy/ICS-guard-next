package guard.ics.backend.audit.controller;

import guard.ics.backend.audit.dto.AuditLogResponse;
import guard.ics.backend.audit.service.AuditLogService;
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

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Operation audit trail")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Operation(summary = "List audit logs with filters")
    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<PageDTO<AuditLogResponse>> list(
            @Parameter(description = "Action performed") @RequestParam(required = false) String action,
            @Parameter(description = "Username who performed the action") @RequestParam(required = false) String username,
            @Parameter(description = "Resource affected") @RequestParam(required = false) String resource,
            @Parameter(description = "Start time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "End time (ISO 8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.success(auditLogService.list(action, username, resource, from, to, pageable));
    }

    @Operation(summary = "Get audit log by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('audit:read')")
    public ApiResponse<AuditLogResponse> get(@Parameter(description = "Audit log ID") @PathVariable Long id) {
        return ApiResponse.success(auditLogService.getById(id));
    }
}
