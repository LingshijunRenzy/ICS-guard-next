package guard.ics.backend.sdn.controller;

import guard.ics.backend.common.exception.BusinessException;
import guard.ics.backend.common.exception.ErrorCode;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.sdn.SdnControllerClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SdnControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SdnControllerClient client;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldReturnHealth() throws Exception {
        when(client.health()).thenReturn(Map.of("status", "UP", "version", "1.0"));

        mockMvc.perform(get("/api/sdn/health").with(user("admin").authorities(() -> "sdn:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void shouldReturn403WithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/sdn/health").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn503WhenUnavailable() throws Exception {
        when(client.health()).thenThrow(new BusinessException(ErrorCode.SDN_CONTROLLER_UNAVAILABLE));

        mockMvc.perform(get("/api/sdn/health").with(user("admin").authorities(() -> "sdn:read")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldBlockFlow() throws Exception {
        when(client.blockFlow(any(), any(), any(), any()))
                .thenReturn(Map.of("flowId", "f-123", "status", "blocked"));

        mockMvc.perform(post("/api/sdn/flows/block")
                .with(user("admin").authorities(() -> "sdn:manage"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"srcIp\":\"10.0.0.1\",\"dstIp\":\"10.0.0.2\",\"protocol\":\"TCP\",\"reason\":\"malicious\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operation").value("blockFlow"));
    }
}
