package guard.ics.backend.metric.service;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.metric.dto.TrafficMetricResponse;
import guard.ics.backend.metric.dto.TrafficStatsResponse;
import guard.ics.backend.metric.entity.TrafficMetricEntity;
import guard.ics.backend.metric.repository.TrafficMetricRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrafficMetricService {

    private final TrafficMetricRepository trafficMetricRepository;

    public TrafficMetricService(TrafficMetricRepository trafficMetricRepository) {
        this.trafficMetricRepository = trafficMetricRepository;
    }

    @Transactional(readOnly = true)
    public PageDTO<TrafficMetricResponse> list(String sourceIp, String destIp, String protocol,
                                                Instant from, Instant to, Pageable pageable) {
        Specification<TrafficMetricEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (sourceIp != null) predicates.add(cb.equal(root.get("sourceIp"), sourceIp));
            if (destIp != null) predicates.add(cb.equal(root.get("destIp"), destIp));
            if (protocol != null) predicates.add(cb.equal(root.get("protocol"), protocol));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("capturedAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("capturedAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<TrafficMetricEntity> page = trafficMetricRepository.findAll(spec, pageable);
        return PageDTO.from(page.map(TrafficMetricResponse::from));
    }

    @Transactional(readOnly = true)
    public TrafficMetricResponse getById(Long id) {
        return TrafficMetricResponse.from(trafficMetricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TrafficMetric", id)));
    }

    @Transactional(readOnly = true)
    public TrafficStatsResponse stats(Instant since) {
        if (since == null) since = Instant.now().minusSeconds(3600);
        Pageable top5 = PageRequest.of(0, 5);

        List<TrafficStatsResponse.TopEntry> topSources = trafficMetricRepository
                .topSourcesByBytes(since, top5).stream()
                .map(row -> new TrafficStatsResponse.TopEntry((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<TrafficStatsResponse.TopEntry> topDests = trafficMetricRepository
                .topDestinationsByBytes(since, top5).stream()
                .map(row -> new TrafficStatsResponse.TopEntry((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<TrafficStatsResponse.ProtocolAggregate> byProto = trafficMetricRepository
                .aggregateByProtocol(since).stream()
                .map(row -> new TrafficStatsResponse.ProtocolAggregate(
                        (String) row[0], ((Number) row[1]).longValue(), ((Number) row[2]).longValue()))
                .toList();

        return new TrafficStatsResponse(topSources, topDests, byProto);
    }
}
