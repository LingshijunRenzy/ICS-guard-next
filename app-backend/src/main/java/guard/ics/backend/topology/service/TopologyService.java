package guard.ics.backend.topology.service;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.topology.dto.TopologyEventResponse;
import guard.ics.backend.topology.entity.TopologyEventEntity;
import guard.ics.backend.topology.repository.TopologyEventRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TopologyService {

    private final TopologyEventRepository topologyEventRepository;

    public TopologyService(TopologyEventRepository topologyEventRepository) {
        this.topologyEventRepository = topologyEventRepository;
    }

    @Transactional(readOnly = true)
    public PageDTO<TopologyEventResponse> listEvents(String eventType, String deviceId, String status,
                                                      Instant from, Instant to, Pageable pageable) {
        Specification<TopologyEventEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventType != null) predicates.add(cb.equal(root.get("eventType"), eventType));
            if (deviceId != null) predicates.add(cb.equal(root.get("deviceId"), deviceId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<TopologyEventEntity> page = topologyEventRepository.findAll(spec, pageable);
        return PageDTO.from(page.map(TopologyEventResponse::from));
    }

    @Transactional(readOnly = true)
    public TopologyEventResponse getEventById(Long id) {
        return TopologyEventResponse.from(topologyEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TopologyEvent", id)));
    }

    @Transactional(readOnly = true)
    public List<TopologyEventResponse> getLatestDevices() {
        return topologyEventRepository.findLatestPerDevice().stream()
                .map(TopologyEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TopologyEventResponse> getDeviceHistory(String deviceId) {
        return topologyEventRepository.findByDeviceIdOrderByOccurredAtDesc(deviceId).stream()
                .map(TopologyEventResponse::from).toList();
    }
}
