package guard.ics.backend.sdn.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "SDN controller operation result")
public record SdnOperationResponse(
        @Schema(description = "Operation performed") String operation,
        @Schema(description = "Result status") String status,
        @Schema(description = "Additional details from the controller") Map<String, Object> details
) {}
