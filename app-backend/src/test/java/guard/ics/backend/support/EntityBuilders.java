package guard.ics.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.UUID;

import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.audit.entity.AuditLogEntity;
import guard.ics.backend.metric.entity.TrafficMetricEntity;
import guard.ics.backend.topology.entity.TopologyEventEntity;

/** Test entity factory methods using the existing Lombok @Builder on each entity. */
public final class EntityBuilders {

    public static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().registerModule(new JavaTimeModule());

    // -- Alert ----------------------------------------------------------------

    public static AlertEntity.AlertEntityBuilder anAlert() {
        return AlertEntity.builder()
                .traceId(UUID.randomUUID().toString())
                .alertType("anomaly_traffic")
                .severity("high")
                .sourceIp("10.0.0.1")
                .destIp("10.0.0.2")
                .protocol("ModbusTCP")
                .description("Test alert")
                .rawPayload("{\"confidence\":0.95}")
                .status("new")
                .triggeredAt(Instant.now())
                .createdAt(Instant.now());
    }

    // -- TrafficMetric --------------------------------------------------------

    public static TrafficMetricEntity.TrafficMetricEntityBuilder aTrafficMetric() {
        return TrafficMetricEntity.builder()
                .traceId(UUID.randomUUID().toString())
                .metricType("flow_stats")
                .sourceIp("10.0.1.1")
                .destIp("10.0.1.2")
                .protocol("ModbusTCP")
                .bytesIn(1024)
                .bytesOut(2048)
                .packetCount(100)
                .flowDuration(500L)
                .capturedAt(Instant.now())
                .createdAt(Instant.now());
    }

    // -- TopologyEvent --------------------------------------------------------

    public static TopologyEventEntity.TopologyEventEntityBuilder aTopologyEvent() {
        return TopologyEventEntity.builder()
                .traceId(UUID.randomUUID().toString())
                .eventType("device_online")
                .deviceId("plc-unit1")
                .deviceName("PLC Unit 1")
                .deviceType("plc")
                .ipAddress("10.0.2.1")
                .macAddress("00:11:22:33:44:55")
                .port("502")
                .status("online")
                .metadata("{\"zone\":\"unit1\",\"changeReason\":\"device_discovered\"}")
                .occurredAt(Instant.now())
                .createdAt(Instant.now());
    }

    // -- AuditLog -------------------------------------------------------------

    public static AuditLogEntity.AuditLogEntityBuilder anAuditLog() {
        return AuditLogEntity.builder()
                .traceId(UUID.randomUUID().toString())
                .userId(1L)
                .username("admin")
                .action("USER_CREATE")
                .resource("User")
                .resourceId("123")
                .detail("{\"createdUserId\":456}")
                .ipAddress("10.0.0.1")
                .createdAt(Instant.now());
    }
}
