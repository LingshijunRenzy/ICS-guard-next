package guard.ics.backend.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import guard.ics.backend.topology.entity.TopologyEventEntity;
import guard.ics.backend.topology.repository.TopologyEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TopologyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TopologyEventConsumer.class);
    private final TopologyEventRepository topologyEventRepository;
    private final ObjectMapper objectMapper;

    public TopologyEventConsumer(TopologyEventRepository topologyEventRepository) {
        this.topologyEventRepository = topologyEventRepository;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @KafkaListener(topics = "ics.topology.events", groupId = "#{T(java.util.UUID).randomUUID().toString()}")
    public void consume(String message) {
        try {
            TopologyEventEntity event = objectMapper.readValue(message, TopologyEventEntity.class);
            event.setId(null);
            topologyEventRepository.save(event);
            log.info("Topology event saved: traceId={}, type={}, device={}",
                    event.getTraceId(), event.getEventType(), event.getDeviceId());
        } catch (Exception e) {
            log.error("Failed to consume topology event", e);
        }
    }
}
