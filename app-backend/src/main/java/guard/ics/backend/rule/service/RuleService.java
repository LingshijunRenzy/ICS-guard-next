package guard.ics.backend.rule.service;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.BadRequestException;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.rule.dto.RuleRequest;
import guard.ics.backend.rule.dto.RuleResponse;
import guard.ics.backend.rule.entity.RuleEntity;
import guard.ics.backend.rule.entity.RuleMetadataEntity;
import guard.ics.backend.rule.repository.RuleRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RuleService {

    private static final Set<String> VALID_RULE_TYPES = Set.of("flow_block", "rate_limit", "traffic_mirror", "alert_suppress");
    private static final Set<String> VALID_ACTIONS = Set.of("block", "allow", "mirror", "log");

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    public PageDTO<RuleResponse> list(String ruleType, String action, Boolean enabled, Pageable pageable) {
        Specification<RuleEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ruleType != null) predicates.add(cb.equal(root.get("ruleType"), ruleType));
            if (action != null) predicates.add(cb.equal(root.get("action"), action));
            if (enabled != null) predicates.add(cb.equal(root.get("enabled"), enabled));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<RuleEntity> page = ruleRepository.findAll(spec, pageable);
        return PageDTO.from(page.map(RuleResponse::from));
    }

    @Transactional(readOnly = true)
    public RuleResponse getById(Long id) {
        return RuleResponse.from(ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id)));
    }

    @Transactional
    public RuleResponse create(RuleRequest request) {
        validate(request);
        RuleEntity entity = toEntity(request);
        entity = ruleRepository.save(entity);
        return RuleResponse.from(entity);
    }

    @Transactional
    public RuleResponse update(Long id, RuleRequest request) {
        validate(request);
        RuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
        apply(request, entity);
        entity = ruleRepository.save(entity);
        return RuleResponse.from(entity);
    }

    @Transactional
    public void delete(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rule", id);
        }
        ruleRepository.deleteById(id);
    }

    @Transactional
    public RuleResponse setEnabled(Long id, boolean enabled) {
        RuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
        entity.setEnabled(enabled);
        return RuleResponse.from(ruleRepository.save(entity));
    }

    @Transactional
    public RuleResponse updatePriority(Long id, int newPriority) {
        RuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule", id));
        entity.setPriority(newPriority);
        return RuleResponse.from(ruleRepository.save(entity));
    }

    private void validate(RuleRequest req) {
        if (!VALID_RULE_TYPES.contains(req.ruleType())) {
            throw new BadRequestException("Invalid rule type: " + req.ruleType());
        }
        if (!VALID_ACTIONS.contains(req.action())) {
            throw new BadRequestException("Invalid action: " + req.action());
        }
    }

    private RuleEntity toEntity(RuleRequest req) {
        RuleEntity entity = RuleEntity.builder()
                .name(req.name()).description(req.description()).ruleType(req.ruleType())
                .target(req.target()).action(req.action()).priority(req.priority())
                .enabled(req.enabled()).ruleConfig(req.ruleConfig()).build();
        if (req.metadata() != null) {
            List<RuleMetadataEntity> metaList = req.metadata().stream().map(m -> {
                RuleMetadataEntity meta = new RuleMetadataEntity();
                meta.setId(new RuleMetadataEntity.RuleMetadataId(null, m.key()));
                meta.setValue(m.value());
                meta.setRule(entity);
                return meta;
            }).collect(Collectors.toList());
            entity.setMetadata(metaList);
        }
        return entity;
    }

    private void apply(RuleRequest req, RuleEntity entity) {
        entity.setName(req.name());
        entity.setDescription(req.description());
        entity.setRuleType(req.ruleType());
        entity.setTarget(req.target());
        entity.setAction(req.action());
        entity.setPriority(req.priority());
        entity.setEnabled(req.enabled());
        entity.setRuleConfig(req.ruleConfig());
        entity.getMetadata().clear();
        if (req.metadata() != null) {
            List<RuleMetadataEntity> metaList = req.metadata().stream().map(m -> {
                RuleMetadataEntity meta = new RuleMetadataEntity();
                meta.setId(new RuleMetadataEntity.RuleMetadataId(entity.getId(), m.key()));
                meta.setValue(m.value());
                meta.setRule(entity);
                return meta;
            }).collect(Collectors.toList());
            entity.getMetadata().addAll(metaList);
        }
    }
}
