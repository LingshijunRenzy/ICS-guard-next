package guard.ics.backend.alert.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "alerts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, unique = true, length = 36)
    private String traceId;

    @Column(name = "alert_type", nullable = false, length = 64)
    private String alertType;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String severity = "medium";

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "dest_ip", length = 45)
    private String destIp;

    @Column(length = 16)
    private String protocol;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "JSONB")
    private String rawPayload;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "new";

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
