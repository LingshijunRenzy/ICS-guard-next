package guard.ics.backend.metric.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "traffic_metrics")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", nullable = false, unique = true, length = 36)
    private String traceId;

    @Column(name = "metric_type", nullable = false, length = 64)
    private String metricType;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "dest_ip", length = 45)
    private String destIp;

    @Column(length = 16)
    private String protocol;

    @Column(name = "bytes_in", nullable = false)
    @Builder.Default
    private long bytesIn = 0;

    @Column(name = "bytes_out", nullable = false)
    @Builder.Default
    private long bytesOut = 0;

    @Column(name = "packet_count", nullable = false)
    @Builder.Default
    private long packetCount = 0;

    @Column(name = "flow_duration")
    private Long flowDuration;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
