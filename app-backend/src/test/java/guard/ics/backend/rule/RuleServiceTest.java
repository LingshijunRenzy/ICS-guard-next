package guard.ics.backend.rule;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rule.dto.RuleRequest;
import guard.ics.backend.rule.dto.RuleResponse;
import guard.ics.backend.rule.entity.RuleEntity;
import guard.ics.backend.rule.repository.RuleRepository;
import guard.ics.backend.rule.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class RuleServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    private RuleService ruleService;

    @BeforeEach
    void setUp() {
        ruleService = new RuleService(ruleRepository);
    }

    @Test
    void shouldCreateRule() {
        RuleRequest request = new RuleRequest("Test Rule", null, "flow_block", "10.0.0.0/8",
                "block", 10, true, "{\"threshold\":100}", null);
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> {
            RuleEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        RuleResponse result = ruleService.create(request);

        assertThat(result.name()).isEqualTo("Test Rule");
        assertThat(result.ruleType()).isEqualTo("flow_block");
        assertThat(result.action()).isEqualTo("block");
    }

    @Test
    void shouldRejectInvalidRuleType() {
        RuleRequest request = new RuleRequest("Bad", null, "invalid_type", null,
                "block", 0, true, null, null);

        assertThatThrownBy(() -> ruleService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectInvalidAction() {
        RuleRequest request = new RuleRequest("Bad", null, "flow_block", null,
                "invalid_action", 0, true, null, null);

        assertThatThrownBy(() -> ruleService.create(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldGetRuleById() {
        RuleEntity entity = RuleEntity.builder()
                .id(1L).name("Test Rule").ruleType("flow_block").action("block").build();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(entity));

        RuleResponse result = ruleService.getById(1L);

        assertThat(result.name()).isEqualTo("Test Rule");
    }

    @Test
    void shouldThrowNotFound() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldToggleEnabled() {
        RuleEntity entity = RuleEntity.builder()
                .id(1L).name("Test").ruleType("flow_block").action("block").enabled(false).build();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any())).thenReturn(entity);

        RuleResponse result = ruleService.setEnabled(1L, true);

        assertThat(result.enabled()).isTrue();
    }

    // ── list ──────────────────────────────────────────────────────

    @Test
    void shouldListRulesWithoutFilters() {
        RuleEntity r1 = RuleEntity.builder().id(1L).name("Rule 1").ruleType("flow_block").action("block").build();
        RuleEntity r2 = RuleEntity.builder().id(2L).name("Rule 2").ruleType("rate_limit").action("allow").build();
        Page<RuleEntity> page = new PageImpl<>(List.of(r1, r2), PageRequest.of(0, 20), 2);
        when(ruleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<RuleResponse> result = ruleService.list(null, null, null, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).name()).isEqualTo("Rule 1");
        assertThat(result.content().get(1).name()).isEqualTo("Rule 2");
    }

    @Test
    void shouldListRulesWithAllFilters() {
        RuleEntity entity = RuleEntity.builder().id(1L).name("Block Rule").ruleType("flow_block")
                .action("block").enabled(true).build();
        Page<RuleEntity> page = new PageImpl<>(List.of(entity));
        when(ruleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<RuleResponse> result = ruleService.list("flow_block", "block", true, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).ruleType()).isEqualTo("flow_block");
        assertThat(result.content().get(0).action()).isEqualTo("block");
        assertThat(result.content().get(0).enabled()).isTrue();
    }

    @Test
    void shouldListRulesWithPartialFilters() {
        Page<RuleEntity> empty = Page.empty();
        when(ruleRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        PageDTO<RuleResponse> result = ruleService.list(null, null, false, PageRequest.of(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // ── update ────────────────────────────────────────────────────

    @Test
    void shouldUpdateRule() {
        RuleEntity existing = RuleEntity.builder()
                .id(1L).name("Old Name").description("Old desc")
                .ruleType("flow_block").target("10.0.0.0/8").action("block")
                .priority(10).enabled(true).ruleConfig("{}").build();
        RuleRequest request = new RuleRequest("New Name", "New desc", "rate_limit",
                "10.0.0.0/16", "allow", 20, false, "{\"limit\":100}", null);

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse result = ruleService.update(1L, request);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.description()).isEqualTo("New desc");
        assertThat(result.ruleType()).isEqualTo("rate_limit");
        assertThat(result.target()).isEqualTo("10.0.0.0/16");
        assertThat(result.action()).isEqualTo("allow");
        assertThat(result.priority()).isEqualTo(20);
        assertThat(result.enabled()).isFalse();
        assertThat(result.ruleConfig()).isEqualTo("{\"limit\":100}");
    }

    @Test
    void shouldThrowNotFoundOnUpdate() {
        RuleRequest request = new RuleRequest("Name", null, "flow_block",
                null, "block", 0, true, null, null);
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdateRuleWithMetadata() {
        RuleEntity existing = RuleEntity.builder()
                .id(1L).name("Old").ruleType("flow_block").action("block").build();
        RuleRequest.MetadataEntry meta = new RuleRequest.MetadataEntry("env", "prod");
        RuleRequest request = new RuleRequest("Name", null, "flow_block",
                null, "block", 0, true, null, List.of(meta));

        when(ruleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse result = ruleService.update(1L, request);

        assertThat(result.metadata()).containsEntry("env", "prod");
    }

    @Test
    void shouldRejectInvalidRuleTypeOnUpdate() {
        RuleRequest request = new RuleRequest("Bad", null, "invalid_type",
                null, "block", 0, true, null, null);

        assertThatThrownBy(() -> ruleService.update(1L, request))
                .isInstanceOf(BadRequestException.class);
    }

    // ── delete ────────────────────────────────────────────────────

    @Test
    void shouldDeleteRule() {
        when(ruleRepository.existsById(1L)).thenReturn(true);

        ruleService.delete(1L);

        verify(ruleRepository).deleteById(1L);
    }

    @Test
    void shouldThrowNotFoundOnDelete() {
        when(ruleRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> ruleService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updatePriority ────────────────────────────────────────────

    @Test
    void shouldUpdatePriority() {
        RuleEntity entity = RuleEntity.builder()
                .id(1L).name("Test").ruleType("flow_block").action("block")
                .priority(10).build();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse result = ruleService.updatePriority(1L, 99);

        assertThat(result.priority()).isEqualTo(99);
    }

    @Test
    void shouldThrowNotFoundOnUpdatePriority() {
        when(ruleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.updatePriority(999L, 50))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUpdatePriorityToZero() {
        RuleEntity entity = RuleEntity.builder()
                .id(1L).name("Test").ruleType("flow_block").action("block")
                .priority(10).build();
        when(ruleRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(ruleRepository.save(any(RuleEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse result = ruleService.updatePriority(1L, 0);

        assertThat(result.priority()).isEqualTo(0);
    }
}
