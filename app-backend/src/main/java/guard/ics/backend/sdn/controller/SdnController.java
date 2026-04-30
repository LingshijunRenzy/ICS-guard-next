package guard.ics.backend.sdn.controller;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.sdn.SdnControllerClient;
import guard.ics.backend.sdn.dto.FlowBlockRequest;
import guard.ics.backend.sdn.dto.SdnOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sdn")
@Tag(name = "SDN Controller", description = "Software-defined networking flow management")
public class SdnController {

    private final SdnControllerClient client;

    public SdnController(SdnControllerClient client) {
        this.client = client;
    }

    @Operation(summary = "Check SDN controller health")
    @GetMapping("/health")
    @PreAuthorize("hasAuthority('sdn:read')")
    public ApiResponse<SdnOperationResponse> health() {
        Map<String, Object> result = client.health();
        return ApiResponse.success(new SdnOperationResponse("health", (String) result.get("status"), result));
    }

    @Operation(summary = "Block a network flow")
    @PostMapping("/flows/block")
    @PreAuthorize("hasAuthority('sdn:manage')")
    public ApiResponse<SdnOperationResponse> blockFlow(@Valid @RequestBody FlowBlockRequest request) {
        Map<String, Object> result = client.blockFlow(request.srcIp(), request.dstIp(),
                request.protocol(), request.reason());
        return ApiResponse.success(new SdnOperationResponse("blockFlow", "completed", result));
    }

    @Operation(summary = "Revoke a flow block rule")
    @DeleteMapping("/flows/{flowId}")
    @PreAuthorize("hasAuthority('sdn:manage')")
    public ApiResponse<SdnOperationResponse> revokeFlow(
            @Parameter(description = "Flow rule ID on the controller") @PathVariable String flowId) {
        Map<String, Object> result = client.revokeFlow(flowId);
        return ApiResponse.success(new SdnOperationResponse("revokeFlow", "completed", result));
    }

    @Operation(summary = "Push a security rule to the SDN controller")
    @PostMapping("/rules/apply")
    @PreAuthorize("hasAuthority('sdn:manage')")
    public ApiResponse<SdnOperationResponse> pushRule(
            @Parameter(description = "Rule ID") @RequestParam Long ruleId,
            @Parameter(description = "Rule configuration JSON") @RequestParam String ruleConfig) {
        Map<String, Object> result = client.pushRule(ruleId, ruleConfig);
        return ApiResponse.success(new SdnOperationResponse("pushRule", "completed", result));
    }
}
