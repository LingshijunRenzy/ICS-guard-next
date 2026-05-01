package guard.ics.backend.rbac.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to update user profile preferences")
public record UpdateProfileRequest(
        @Schema(description = "UI language", example = "zh")
        @Pattern(regexp = "en|zh", message = "language must be 'en' or 'zh'")
        String language,

        @Schema(description = "Timezone", example = "Asia/Shanghai")
        String timezone,

        @Schema(description = "UI theme", example = "dark")
        @Pattern(regexp = "light|dark", message = "theme must be 'light' or 'dark'")
        String theme
) {}
