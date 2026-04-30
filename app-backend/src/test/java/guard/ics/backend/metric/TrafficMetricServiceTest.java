package guard.ics.backend.metric;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.metric.dto.TrafficMetricResponse;
import guard.ics.backend.metric.dto.TrafficStatsResponse;
import guard.ics.backend.metric.entity.TrafficMetricEntity;
import guard.ics.backend.metric.repository.TrafficMetricRepository;
import guard.ics.backend.metric.service.TrafficMetricService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficMetricServiceTest {

    @Mock
    private TrafficMetricRepository trafficMetricRepository;

    private TrafficMetricService trafficMetricService;

    @BeforeEach
    void setUp() {
        trafficMetricService = new TrafficMetricService(trafficMetricRepository);
    }

    // ── list ──────────────────────────────────────────────────────

    @Test
    void shouldListTrafficMetricsWithAllFilters() {
        TrafficMetricEntity entity = createEntity(1L, "flow_stats", "10.0.0.1", "10.0.0.2", "TCP");
        Page<TrafficMetricEntity> page = new PageImpl<>(List.of(entity));
        when(trafficMetricRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TrafficMetricResponse> result = trafficMetricService.list(
                "10.0.0.1", "10.0.0.2", "TCP",
                Instant.now().minusSeconds(3600), Instant.now(), PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        TrafficMetricResponse r = result.content().get(0);
        assertThat(r.sourceIp()).isEqualTo("10.0.0.1");
        assertThat(r.destIp()).isEqualTo("10.0.0.2");
        assertThat(r.protocol()).isEqualTo("TCP");
        assertThat(r.bytesIn()).isEqualTo(1000);
        assertThat(r.bytesOut()).isEqualTo(500);
        assertThat(r.packetCount()).isEqualTo(100);
        assertThat(r.flowDuration()).isEqualTo(1500);
    }

    @Test
    void shouldListTrafficMetricsWithoutFilters() {
        TrafficMetricEntity e1 = createEntity(1L, "flow_stats", "10.0.0.1", "10.0.0.2", "TCP");
        TrafficMetricEntity e2 = createEntity(2L, "flow_stats", "10.0.0.3", "10.0.0.4", "UDP");
        Page<TrafficMetricEntity> page = new PageImpl<>(List.of(e1, e2));
        when(trafficMetricRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TrafficMetricResponse> result = trafficMetricService.list(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    @Test
    void shouldReturnEmptyTrafficMetricPage() {
        Page<TrafficMetricEntity> empty = Page.empty();
        when(trafficMetricRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        PageDTO<TrafficMetricResponse> result = trafficMetricService.list(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void shouldListTrafficMetricsWithPartialFilters() {
        TrafficMetricEntity entity = createEntity(1L, "flow_stats", "10.0.0.1", "10.0.0.2", "TCP");
        Page<TrafficMetricEntity> page = new PageImpl<>(List.of(entity));
        when(trafficMetricRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TrafficMetricResponse> result = trafficMetricService.list(
                "10.0.0.1", null, "TCP", null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).sourceIp()).isEqualTo("10.0.0.1");
        assertThat(result.content().get(0).protocol()).isEqualTo("TCP");
    }

    // ── getById ───────────────────────────────────────────────────

    @Test
    void shouldGetTrafficMetricById() {
        TrafficMetricEntity entity = createEntity(1L, "flow_stats", "10.0.0.1", "10.0.0.2", "TCP");
        when(trafficMetricRepository.findById(1L)).thenReturn(Optional.of(entity));

        TrafficMetricResponse result = trafficMetricService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.traceId()).isEqualTo("trace-1");
        assertThat(result.metricType()).isEqualTo("flow_stats");
        assertThat(result.sourceIp()).isEqualTo("10.0.0.1");
        assertThat(result.destIp()).isEqualTo("10.0.0.2");
        assertThat(result.protocol()).isEqualTo("TCP");
        assertThat(result.capturedAt()).isNotNull();
    }

    @Test
    void shouldThrowNotFoundWhenTrafficMetricMissing() {
        when(trafficMetricRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trafficMetricService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── stats ─────────────────────────────────────────────────────

    @Test
    void shouldReturnTrafficStatsWithDefaultTimeRange() {
        Instant now = Instant.now();
        List<Object[]> topSources = List.<Object[]>of(new Object[]{"10.0.0.1", 5000L}, new Object[]{"10.0.0.2", 3000L});
        List<Object[]> topDests = List.<Object[]>of(new Object[]{"10.0.0.3", 4000L});
        List<Object[]> byProto = List.<Object[]>of(new Object[]{"TCP", 6000L, 10L}, new Object[]{"UDP", 2000L, 3L});

        when(trafficMetricRepository.topSourcesByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(topSources);
        when(trafficMetricRepository.topDestinationsByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(topDests);
        when(trafficMetricRepository.aggregateByProtocol(any(Instant.class)))
                .thenReturn(byProto);

        TrafficStatsResponse result = trafficMetricService.stats(null);

        assertThat(result.topSources()).hasSize(2);
        assertThat(result.topSources().get(0).ip()).isEqualTo("10.0.0.1");
        assertThat(result.topSources().get(0).totalBytes()).isEqualTo(5000L);
        assertThat(result.topSources().get(1).ip()).isEqualTo("10.0.0.2");
        assertThat(result.topSources().get(1).totalBytes()).isEqualTo(3000L);

        assertThat(result.topDestinations()).hasSize(1);
        assertThat(result.topDestinations().get(0).ip()).isEqualTo("10.0.0.3");
        assertThat(result.topDestinations().get(0).totalBytes()).isEqualTo(4000L);

        assertThat(result.byProtocol()).hasSize(2);
        assertThat(result.byProtocol().get(0).protocol()).isEqualTo("TCP");
        assertThat(result.byProtocol().get(0).totalBytes()).isEqualTo(6000L);
        assertThat(result.byProtocol().get(0).flows()).isEqualTo(10L);
        assertThat(result.byProtocol().get(1).protocol()).isEqualTo("UDP");
        assertThat(result.byProtocol().get(1).totalBytes()).isEqualTo(2000L);
        assertThat(result.byProtocol().get(1).flows()).isEqualTo(3L);
    }

    @Test
    void shouldReturnTrafficStatsWithExplicitSince() {
        Instant since = Instant.now().minusSeconds(7200);
        List<Object[]> empty = List.of();
        when(trafficMetricRepository.topSourcesByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(empty);
        when(trafficMetricRepository.topDestinationsByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(empty);
        when(trafficMetricRepository.aggregateByProtocol(any(Instant.class)))
                .thenReturn(empty);

        TrafficStatsResponse result = trafficMetricService.stats(since);

        assertThat(result.topSources()).isEmpty();
        assertThat(result.topDestinations()).isEmpty();
        assertThat(result.byProtocol()).isEmpty();
    }

    @Test
    void shouldReturnStatsWithEmptyResults() {
        List<Object[]> empty = List.of();
        when(trafficMetricRepository.topSourcesByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(empty);
        when(trafficMetricRepository.topDestinationsByBytes(any(Instant.class), any(Pageable.class)))
                .thenReturn(empty);
        when(trafficMetricRepository.aggregateByProtocol(any(Instant.class)))
                .thenReturn(empty);

        TrafficStatsResponse result = trafficMetricService.stats(null);

        assertThat(result.topSources()).isEmpty();
        assertThat(result.topDestinations()).isEmpty();
        assertThat(result.byProtocol()).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────

    private TrafficMetricEntity createEntity(Long id, String metricType, String srcIp, String dstIp, String protocol) {
        return TrafficMetricEntity.builder()
                .id(id).traceId("trace-" + id).metricType(metricType)
                .sourceIp(srcIp).destIp(dstIp).protocol(protocol)
                .bytesIn(1000).bytesOut(500).packetCount(100)
                .flowDuration(1500L)
                .capturedAt(Instant.now()).createdAt(Instant.now()).build();
    }
}
