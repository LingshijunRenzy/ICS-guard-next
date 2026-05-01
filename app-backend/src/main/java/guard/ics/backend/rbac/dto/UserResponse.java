package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "User account information")
public record UserResponse(
        @Schema(description = "User ID") Long id,
        @Schema(description = "Login username") String username,
        @Schema(description = "Email address") String email,
        @Schema(description = "Display name") String displayName,
        @Schema(description = "Whether the account is enabled") boolean enabled,
        @Schema(description = "Assigned role names") Set<String> roles,
        @Schema(description = "Account creation time") Instant createdAt,
        @Schema(description = "Last update time") Instant updatedAt,
        @Schema(description = "User preference profile") UserProfileResponse profile
) {}
