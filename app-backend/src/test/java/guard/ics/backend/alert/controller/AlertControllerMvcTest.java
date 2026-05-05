package guard.ics.backend.alert.controller;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.service.AlertService;
import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
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
class AlertControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @MockitoBean
    private AuditLogProducer auditLogProducer;

    @MockitoBean
    private ModelEventProducer modelEventProducer;

    // ------- GET /api/alerts -------

    @Test
    void shouldListAlerts() throws Exception {
        var response = new AlertResponse(1L, "tr1", "anomaly", "high", "10.0.0.1",
                "10.0.0.2", "TCP", "desc", "new", Instant.now(), Instant.now());
        when(alertService.list(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageDTO<>(List.of(response), 0, 20, 1, 1));

        mockMvc.perform(get("/api/alerts").with(user("admin").authorities(() -> "alerts:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].traceId").value("tr1"));
    }

    @Test
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void shouldReturn403WithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/alerts").with(user("user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ------- GET /api/alerts/{id} -------

    @Test
    void shouldGetAlertById() throws Exception {
        var response = new AlertResponse(42L, "tid", "scanning", "medium",
                "10.0.0.1", "10.0.0.2", "TCP", "desc", "new", Instant.now(), Instant.now());
        when(alertService.getById(42L)).thenReturn(response);

        mockMvc.perform(get("/api/alerts/42").with(user("admin").authorities(() -> "alerts:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(42));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(alertService.getById(999L)).thenThrow(new ResourceNotFoundException("Alert", 999L));

        mockMvc.perform(get("/api/alerts/999").with(user("admin").authorities(() -> "alerts:read")))
                .andExpect(status().isNotFound());
    }

    // ------- PATCH /api/alerts/{id}/status -------

    @Test
    void shouldTransitionStatus() throws Exception {
        var response = new AlertResponse(1L, "tid", "anomaly", "high",
                "10.0.0.1", "10.0.0.2", "TCP", "desc", "acknowledged", Instant.now(), Instant.now());
        when(alertService.transitionStatus(1L, "acknowledged")).thenReturn(response);

        mockMvc.perform(patch("/api/alerts/1/status")
                        .with(user("admin").authorities(() -> "alerts:manage"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"status\":\"acknowledged\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("acknowledged"));
    }

    // ------- POST /api/alerts/{id}/escalate -------

    @Test
    void shouldEscalateAlert() throws Exception {
        var response = new AlertResponse(1L, "tid", "anomaly", "critical",
                "10.0.0.1", "10.0.0.2", "TCP", "desc", "escalated", Instant.now(), Instant.now());
        when(alertService.escalate(1L)).thenReturn(response);

        mockMvc.perform(post("/api/alerts/1/escalate")
                        .with(user("admin").authorities(() -> "alerts:manage"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.severity").value("critical"));
    }

    // ------- GET /api/alerts/stats -------

    @Test
    void shouldReturnStats() throws Exception {
        when(alertService.stats(any())).thenReturn(new AlertStatsResponse(
                Map.of("high", 30L, "low", 70L), Map.of("new", 50L, "resolved", 50L), 100L));

        mockMvc.perform(get("/api/alerts/stats").with(user("admin").authorities(() -> "alerts:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(100));
    }
}
