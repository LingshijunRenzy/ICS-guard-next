package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.rbac.service.RoleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoleControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RoleService roleService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldListRoles() throws Exception {
        mockMvc.perform(get("/api/roles").with(user("admin").authorities(() -> "users:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403WithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/roles").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(roleService.getById(99L)).thenThrow(new ResourceNotFoundException("Role", 99L));

        mockMvc.perform(get("/api/roles/99").with(user("admin").authorities(() -> "users:read")))
                .andExpect(status().isNotFound());
    }
}
