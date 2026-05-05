package guard.ics.backend.topology.controller;

import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.topology.service.TopologyService;
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
class TopologyControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TopologyService topologyService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldListEvents() throws Exception {
        when(topologyService.listEvents(any(), any(), any(), any(), any(), any())).thenReturn(null);

        mockMvc.perform(get("/api/topology/events").with(user("admin").authorities(() -> "topology:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/topology/events").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnDevices() throws Exception {
        mockMvc.perform(get("/api/topology/devices").with(user("admin").authorities(() -> "topology:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
