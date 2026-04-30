package guard.ics.backend.sdn;

import guard.ics.backend.common.dto.ApiResponse;
import guard.ics.backend.common.exception.BusinessException;
import guard.ics.backend.common.exception.ErrorCode;
import guard.ics.backend.sdn.controller.SdnController;
import guard.ics.backend.sdn.dto.FlowBlockRequest;
import guard.ics.backend.sdn.dto.SdnOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SdnControllerTest {

    @Mock
    private SdnControllerClient client;

    private SdnController controller;

    @BeforeEach
    void setUp() {
        controller = new SdnController(client);
    }

    @Test
    void shouldReturnHealth() {
        Map<String, Object> healthResult = Map.of("status", "UP", "version", "1.0");
        when(client.health()).thenReturn(healthResult);

        ApiResponse<SdnOperationResponse> result = controller.health();

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().operation()).isEqualTo("health");
        assertThat(result.data().status()).isEqualTo("UP");
    }

    @Test
    void shouldReturnHealthWhenDown() {
        Map<String, Object> downResult = Map.of("status", "DOWN", "error", "timeout");
        when(client.health()).thenReturn(downResult);

        ApiResponse<SdnOperationResponse> result = controller.health();

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().status()).isEqualTo("DOWN");
    }

    @Test
    void shouldBlockFlow() {
        FlowBlockRequest request = new FlowBlockRequest("10.0.0.1", "10.0.0.2", "TCP", "malicious");
        Map<String, Object> blockResult = Map.of("flowId", "f-123", "status", "blocked");
        when(client.blockFlow("10.0.0.1", "10.0.0.2", "TCP", "malicious")).thenReturn(blockResult);

        ApiResponse<SdnOperationResponse> result = controller.blockFlow(request);

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().operation()).isEqualTo("blockFlow");
        assertThat(result.data().status()).isEqualTo("completed");
        assertThat(result.data().details()).containsEntry("flowId", "f-123");
    }

    @Test
    void shouldRevokeFlow() {
        Map<String, Object> revokeResult = Map.of("flowId", "f-123", "status", "revoked");
        when(client.revokeFlow("f-123")).thenReturn(revokeResult);

        ApiResponse<SdnOperationResponse> result = controller.revokeFlow("f-123");

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().operation()).isEqualTo("revokeFlow");
    }

    @Test
    void shouldPushRule() {
        Map<String, Object> pushResult = Map.of("ruleId", 1L, "status", "applied");
        when(client.pushRule(1L, "{\"threshold\":100}")).thenReturn(pushResult);

        ApiResponse<SdnOperationResponse> result = controller.pushRule(1L, "{\"threshold\":100}");

        assertThat(result.code()).isEqualTo(200);
        assertThat(result.data().operation()).isEqualTo("pushRule");
    }

    @Test
    void shouldReturn503WhenControllerUnavailable() {
        FlowBlockRequest request = new FlowBlockRequest("10.0.0.1", "10.0.0.2", "TCP", "test");
        when(client.blockFlow(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE));

        assertThatThrownBy(() -> controller.blockFlow(request))
                .isInstanceOf(BusinessException.class);
    }
}
