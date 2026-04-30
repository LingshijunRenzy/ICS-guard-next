package guard.ics.backend.rule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rules")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_type", nullable = false, length = 64)
    private String ruleType;

    @Column(length = 256)
    private String target;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "rule_config", columnDefinition = "JSONB")
    private String ruleConfig;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RuleMetadataEntity> metadata = new ArrayList<>();
}
