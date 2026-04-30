package guard.ics.backend.rule;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
}
