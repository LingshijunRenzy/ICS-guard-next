package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permission type category")
public record PermissionTypeResponse(
        @Schema(description = "Type ID") Long id,
        @Schema(description = "Type name") String name,
        @Schema(description = "Type description") String description
) {}
