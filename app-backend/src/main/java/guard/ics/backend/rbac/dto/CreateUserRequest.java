package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "User creation / update request")
public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Schema(description = "Login username", example = "operator1") String username,
        @NotBlank @Size(min = 6, max = 128)
        @Schema(description = "Password (min 6 chars)", example = "securePass!") String password,
        @Email @Schema(description = "Email address", example = "op@ics-guard.local") String email,
        @Schema(description = "Display name") String displayName,
        @Schema(description = "Role IDs to assign") Set<Long> roleIds
) {}
