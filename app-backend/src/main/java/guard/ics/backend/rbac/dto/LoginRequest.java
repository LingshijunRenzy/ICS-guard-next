package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(
        @NotBlank @Schema(description = "Username", example = "admin") String username,
        @NotBlank @Schema(description = "Password", example = "admin123") String password
) {}
