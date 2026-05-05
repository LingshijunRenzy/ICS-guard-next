package guard.ics.backend.dashboard.controller;

import guard.ics.backend.dashboard.service.DashboardService;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldReturnDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("admin").authorities(() -> "dashboard:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("user")))
                .andExpect(status().isForbidden());
    }
}
