package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permission point (e.g. alerts:read)")
public record PermissionResponse(
        @Schema(description = "Permission ID") Long id,
        @Schema(description = "Permission identifier (resource:action)") String name,
        @Schema(description = "Description") String description
) {}
