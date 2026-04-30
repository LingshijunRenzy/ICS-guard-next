package guard.ics.backend.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "Rule creation / update request")
public record RuleRequest(
        @NotBlank @Schema(description = "Rule name", example = "Block inbound SSH") String name,
        @Schema(description = "Description") String description,
        @NotBlank @Schema(description = "Rule type: flow_block | rate_limit | traffic_mirror | alert_suppress") String ruleType,
        @Schema(description = "Target IP/CIDR or device ID") String target,
        @NotBlank @Schema(description = "Action: block | allow | mirror | log") String action,
        @Schema(description = "Priority (higher = evaluated first)", example = "100") int priority,
        @Schema(description = "Whether the rule is active", example = "true") boolean enabled,
        @Schema(description = "JSON rule configuration") String ruleConfig,
        @Schema(description = "Metadata key-value pairs") List<MetadataEntry> metadata
) {
    @Schema(description = "Metadata key-value entry")
    public record MetadataEntry(
            @NotBlank @Schema(description = "Key") String key,
            @NotBlank @Schema(description = "Value") String value
    ) {}
}
