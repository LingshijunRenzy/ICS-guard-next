package guard.ics.backend.rbac.controller;

import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.rbac.dto.PermissionResponse;
import guard.ics.backend.rbac.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PermissionControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PermissionService permissionService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    @Test
    void shouldListPermissions() throws Exception {
        when(permissionService.list()).thenReturn(List.of(
                new PermissionResponse(1L, "alerts:read", "Read alerts", null, null, null, null, null)));

        mockMvc.perform(get("/api/permissions").with(user("admin").authorities(() -> "permissions:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("alerts:read"));
    }

    @Test
    void shouldReturn403WithoutAuth() throws Exception {
        mockMvc.perform(get("/api/permissions").with(user("user")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreatePermission() throws Exception {
        when(permissionService.create(eq("new:perm"), eq("New permission"), any(), any()))
                .thenReturn(new PermissionResponse(14L, "new:perm", "New permission", null, null, null,
                        Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-01T00:00:00Z")));

        mockMvc.perform(post("/api/permissions")
                        .with(user("admin").authorities(() -> "permissions:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new:perm\",\"description\":\"New permission\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("new:perm"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    void shouldCreatePermissionWithMetadata() throws Exception {
        when(permissionService.create(eq("meta:perm"), any(), eq(Map.of("category", "core")), any()))
                .thenReturn(new PermissionResponse(15L, "meta:perm", "Desc", Map.of("category", "core"), null, null, null, null));

        mockMvc.perform(post("/api/permissions")
                        .with(user("admin").authorities(() -> "permissions:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"meta:perm\",\"metadata\":{\"category\":\"core\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.metadata.category").value("core"));
    }

    @Test
    void shouldUpdatePermission() throws Exception {
        when(permissionService.update(eq(1L), eq("updated:perm"), any(), any(), any()))
                .thenReturn(new PermissionResponse(1L, "updated:perm", "Updated", null, null, null, null, null));

        mockMvc.perform(put("/api/permissions/1")
                        .with(user("admin").authorities(() -> "permissions:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"updated:perm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("updated:perm"));
    }

    @Test
    void shouldDeletePermission() throws Exception {
        doNothing().when(permissionService).delete(1L);

        mockMvc.perform(delete("/api/permissions/1")
                        .with(user("admin").authorities(() -> "permissions:manage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void shouldReturn403OnCreateWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/permissions")
                        .with(user("viewer").authorities(() -> "permissions:read"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new:perm\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenUpdatingNonexistent() throws Exception {
        when(permissionService.update(eq(99L), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Permission", 99L));

        mockMvc.perform(put("/api/permissions/99")
                        .with(user("admin").authorities(() -> "permissions:manage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ghost\"}"))
                .andExpect(status().isNotFound());
    }
}
