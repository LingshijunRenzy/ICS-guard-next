package guard.ics.backend.rbac.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Permission point (e.g. alerts:read)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PermissionResponse(
        @Schema(description = "Permission ID") Long id,
        @Schema(description = "Permission identifier (resource:action)") String name,
        @Schema(description = "Description") String description,
        @Schema(description = "Key-value metadata") Map<String, String> metadata,
        @Schema(description = "Type ID") Long typeId,
        @Schema(description = "Type name") String typeName,
        @Schema(description = "Creation time") Instant createdAt,
        @Schema(description = "Last update time") Instant updatedAt
) {}
