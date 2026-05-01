package guard.ics.backend.rbac.dto;

import guard.ics.backend.rbac.entity.UserProfileEntity;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User preference profile")
public record UserProfileResponse(
        @Schema(description = "UI language preference") String language,
        @Schema(description = "User timezone") String timezone,
        @Schema(description = "UI theme preference") String theme
) {
    public static UserProfileResponse from(UserProfileEntity p) {
        if (p == null) return null;
        return new UserProfileResponse(p.getLanguage(), p.getTimezone(), p.getTheme());
    }
}
