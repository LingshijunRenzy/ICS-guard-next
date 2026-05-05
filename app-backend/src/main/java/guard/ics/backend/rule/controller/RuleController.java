package guard.ics.backend.rule.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.rule.dto.RuleRequest;
import guard.ics.backend.rule.dto.RuleResponse;
import guard.ics.backend.rule.service.RuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rules")
@Tag(name = "Rules", description = "Security policy rule management")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @Operation(summary = "List rules with filters")
    @GetMapping
    @PreAuthorize("hasAuthority('rules:read')")
    public ApiResponse<PageDTO<RuleResponse>> list(
            @Parameter(description = "Rule type: flow_block, rate_limit, traffic_mirror, alert_suppress") @RequestParam(required = false) String ruleType,
            @Parameter(description = "Action: block, allow, mirror, log") @RequestParam(required = false) String action,
            @Parameter(description = "Filter by enabled status") @RequestParam(required = false) Boolean enabled,
            @PageableDefault(size = 20, sort = "priority") Pageable pageable) {
        return ApiResponse.success(ruleService.list(ruleType, action, enabled, pageable));
    }

    @Operation(summary = "Get rule by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('rules:read')")
    public ApiResponse<RuleResponse> get(@Parameter(description = "Rule ID") @PathVariable Long id) {
        return ApiResponse.success(ruleService.getById(id));
    }

    @Operation(summary = "Create new rule")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid rule config")})
    @PostMapping
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<RuleResponse> create(@Valid @RequestBody RuleRequest request) {
        return ApiResponse.created(ruleService.create(request));
    }

    @Operation(summary = "Update existing rule")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<RuleResponse> update(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        return ApiResponse.success(ruleService.update(id, request));
    }

    @Operation(summary = "Delete rule")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200"), @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404")})
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ruleService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "Enable rule")
    @PatchMapping("/{id}/enable")
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<RuleResponse> enable(@PathVariable Long id) {
        return ApiResponse.success(ruleService.setEnabled(id, true));
    }

    @Operation(summary = "Disable rule")
    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<RuleResponse> disable(@PathVariable Long id) {
        return ApiResponse.success(ruleService.setEnabled(id, false));
    }

    @Operation(summary = "Update rule priority")
    @PutMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('rules:manage')")
    public ApiResponse<RuleResponse> updatePriority(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return ApiResponse.success(ruleService.updatePriority(id, body.get("priority")));
    }
}
