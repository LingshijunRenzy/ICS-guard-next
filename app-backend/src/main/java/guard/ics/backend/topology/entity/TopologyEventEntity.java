package guard.ics.backend.topology.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "topology_events")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopologyEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, unique = true, length = 36)
    private String traceId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_name", length = 256)
    private String deviceName;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(length = 32)
    private String port;

    @Column(length = 32)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
