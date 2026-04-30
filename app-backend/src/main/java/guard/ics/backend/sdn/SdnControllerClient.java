package guard.ics.backend.sdn;

import guard.ics.backend.common.exception.BusinessException;
import guard.ics.backend.common.exception.ErrorCode;
import guard.ics.backend.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class SdnControllerClient {

    private static final Logger log = LoggerFactory.getLogger(SdnControllerClient.class);
    private final RestClient restClient;

    public SdnControllerClient(AppProperties appProperties) {
        this.restClient = RestClient.builder().baseUrl(appProperties.controller().baseUrl()).build();
    }

    public Map<String, Object> blockFlow(String srcIp, String dstIp, String protocol, String reason) {
        log.info("Blocking flow: {} -> {} ({}) — {}", srcIp, dstIp, protocol, reason);
        try {
            return restClient.post()
                    .uri("/api/sdn/flows/block")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("srcIp", srcIp, "dstIp", dstIp, "protocol", protocol, "reason", reason))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed to block flow via SDN controller", e);
            throw new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE);
        }
    }

    public Map<String, Object> revokeFlow(String flowId) {
        log.info("Revoking flow: {}", flowId);
        try {
            return restClient.delete()
                    .uri("/api/sdn/flows/{flowId}", flowId)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed to revoke flow via SDN controller", e);
            throw new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE);
        }
    }

    public Map<String, Object> pushRule(Long ruleId, String ruleConfig) {
        log.info("Pushing rule {} to controller", ruleId);
        try {
            return restClient.post()
                    .uri("/api/sdn/rules/apply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("ruleId", ruleId, "config", ruleConfig))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Failed to push rule to SDN controller", e);
            throw new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE);
        }
    }

    public Map<String, Object> health() {
        try {
            return restClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("SDN controller health check failed: {}", e.getMessage());
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }
}
