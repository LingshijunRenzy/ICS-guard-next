package guard.ics.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import guard.ics.backend.metric.entity.TrafficMetricEntity;
import guard.ics.backend.metric.repository.TrafficMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TrafficMetricConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrafficMetricConsumer.class);
    private final TrafficMetricRepository trafficMetricRepository;
    private final ObjectMapper objectMapper;

    public TrafficMetricConsumer(TrafficMetricRepository trafficMetricRepository) {
        this.trafficMetricRepository = trafficMetricRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "ics.traffic.metrics", groupId = "traffic-metrics-consumer")
    public void consume(String message) {
        try {
            TrafficMetricEntity metric = objectMapper.readValue(message, TrafficMetricEntity.class);
            if (trafficMetricRepository.findByTraceId(metric.getTraceId()).isPresent()) {
                log.debug("Duplicate traffic metric skipped: traceId={}", metric.getTraceId());
                return;
            }
            metric.setId(null);
            trafficMetricRepository.save(metric);
            log.debug("Traffic metric saved: traceId={}, {}:{} -> {}", metric.getTraceId(),
                    metric.getSourceIp(), metric.getDestIp(), metric.getProtocol());
        } catch (Exception e) {
            log.error("Failed to consume traffic metric", e);
        }
    }
}
