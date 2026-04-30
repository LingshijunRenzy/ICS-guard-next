package guard.ics.backend.rule.dto;

import guard.ics.backend.rule.entity.RuleEntity;
import guard.ics.backend.rule.entity.RuleMetadataEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "Rule details with metadata")
public record RuleResponse(
        @Schema(description = "Rule ID") Long id,
        @Schema(description = "Rule name") String name,
        @Schema(description = "Description") String description,
        @Schema(description = "Rule type") String ruleType,
        @Schema(description = "Target IP/CIDR or device ID") String target,
        @Schema(description = "Action") String action,
        @Schema(description = "Priority (higher = first)") int priority,
        @Schema(description = "Whether the rule is active") boolean enabled,
        @Schema(description = "JSON rule configuration") String ruleConfig,
        @Schema(description = "Creator username") String createdBy,
        @Schema(description = "Creation time") Instant createdAt,
        @Schema(description = "Last update time") Instant updatedAt,
        @Schema(description = "Metadata key-value pairs") Map<String, String> metadata
) {
    public static RuleResponse from(RuleEntity e) {
        Map<String, String> meta = e.getMetadata().stream()
                .collect(Collectors.toMap(m -> m.getId().getKey(), RuleMetadataEntity::getValue));
        return new RuleResponse(e.getId(), e.getName(), e.getDescription(), e.getRuleType(),
                e.getTarget(), e.getAction(), e.getPriority(), e.isEnabled(), e.getRuleConfig(),
                e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedAt(), meta);
    }
}
