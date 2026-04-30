package guard.ics.backend.rule.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "rule_metadata")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleMetadataEntity {

    @EmbeddedId
    private RuleMetadataId id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @MapsId("ruleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    @JsonIgnore
    private RuleEntity rule;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class RuleMetadataId implements Serializable {
        @Column(name = "rule_id")
        private Long ruleId;
        @Column(length = 128)
        private String key;
    }
}
