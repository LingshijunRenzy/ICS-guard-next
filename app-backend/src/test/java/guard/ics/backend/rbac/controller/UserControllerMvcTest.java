package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.rbac.service.UserService;
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
class UserControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldReturn401WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/users").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        when(userService.getById(99L)).thenThrow(new ResourceNotFoundException("User", 99L));

        mockMvc.perform(get("/api/users/99").with(user("admin").authorities(() -> "users:read")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListUsersWithAuth() throws Exception {
        when(userService.list(any())).thenReturn(null);

        mockMvc.perform(get("/api/users").with(user("admin").authorities(() -> "users:read")))
                .andExpect(status().isOk());
    }
}
