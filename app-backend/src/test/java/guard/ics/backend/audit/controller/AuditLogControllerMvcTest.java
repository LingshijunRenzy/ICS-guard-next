package guard.ics.backend.audit.controller;

import guard.ics.backend.audit.service.AuditLogService;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldListAuditLogs() throws Exception {
        when(auditLogService.list(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/audit-logs").with(user("admin").authorities(() -> "audit:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/audit-logs").with(user("user")))
                .andExpect(status().isForbidden());
    }
}
