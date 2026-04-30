package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Role with assigned permissions")
public record RoleResponse(
        @Schema(description = "Role ID") Long id,
        @Schema(description = "Role name (e.g. ROLE_OPERATOR)") String name,
        @Schema(description = "Description") String description,
        @Schema(description = "Permission names assigned to this role") Set<String> permissions
) {}
