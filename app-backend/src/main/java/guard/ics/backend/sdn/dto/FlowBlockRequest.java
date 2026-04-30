package guard.ics.backend.sdn.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to block a network flow")
public record FlowBlockRequest(
        @NotBlank @Schema(description = "Source IP address", example = "10.0.0.1") String srcIp,
        @NotBlank @Schema(description = "Destination IP address", example = "10.0.0.2") String dstIp,
        @NotBlank @Schema(description = "Protocol", example = "TCP") String protocol,
        @NotBlank @Schema(description = "Reason for blocking the flow") String reason
) {}
