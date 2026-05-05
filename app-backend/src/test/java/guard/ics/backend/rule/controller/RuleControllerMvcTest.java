package guard.ics.backend.rule.controller;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.kafka.producer.AuditLogProducer;
import guard.ics.backend.kafka.producer.ModelEventProducer;
import guard.ics.backend.rule.dto.RuleResponse;
import guard.ics.backend.rule.service.RuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class RuleControllerMvcTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private RuleService ruleService;
    @MockitoBean private AuditLogProducer auditLogProducer;
    @MockitoBean private ModelEventProducer modelEventProducer;

    private static RuleResponse ruleResp(boolean enabled) {
        return new RuleResponse(1L, "Block HTTP", "desc", "traffic_filter",
                "10.0.0.0/24", "block", 10, enabled, "{}",
                "admin", Instant.now(), Instant.now(), Map.of());
    }

    @Test
    void shouldListRules() throws Exception {
        when(ruleService.list(any(), any(), any(), any()))
                .thenReturn(new PageDTO<>(List.of(ruleResp(true)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/rules").with(user("admin").authorities(() -> "policies:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Block HTTP"));
    }

    @Test
    void shouldReturn403WithoutPolicyRead() throws Exception {
        mockMvc.perform(get("/api/rules").with(user("user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void shouldGetRuleById() throws Exception {
        when(ruleService.getById(1L)).thenReturn(ruleResp(true));

        mockMvc.perform(get("/api/rules/1").with(user("admin").authorities(() -> "policies:read")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void shouldReturn404WhenRuleNotFound() throws Exception {
        when(ruleService.getById(99L)).thenThrow(new ResourceNotFoundException("Rule", 99L));

        mockMvc.perform(get("/api/rules/99").with(user("admin").authorities(() -> "policies:read")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldEnableRule() throws Exception {
        when(ruleService.setEnabled(1L, true)).thenReturn(ruleResp(true));

        mockMvc.perform(patch("/api/rules/1/enable")
                .with(user("admin").authorities(() -> "policies:manage")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void shouldDisableRule() throws Exception {
        when(ruleService.setEnabled(1L, false)).thenReturn(ruleResp(false));

        mockMvc.perform(patch("/api/rules/1/disable")
                .with(user("admin").authorities(() -> "policies:manage")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }
}
