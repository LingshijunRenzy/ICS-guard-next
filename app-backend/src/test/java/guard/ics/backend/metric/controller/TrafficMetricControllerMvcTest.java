package guard.ics.backend.metric.controller;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.metric.dto.TrafficMetricResponse;
import guard.ics.backend.metric.service.TrafficMetricService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TrafficMetricControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TrafficMetricService trafficMetricService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldListMetrics() throws Exception {
        var m = new TrafficMetricResponse(1L, "tr1", "flow_stats", "10.0.0.1", "10.0.0.2", "ModbusTCP", 1024L, 2048L, 100L, 500L, Instant.now(), Instant.now());
        when(trafficMetricService.list(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageDTO<>(List.of(m), 0, 20, 1, 1));

        mockMvc.perform(get("/api/metrics/traffic").with(user("admin").authorities(() -> "metrics:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/metrics/traffic").with(user("user")))
                .andExpect(status().isForbidden());
    }
}
