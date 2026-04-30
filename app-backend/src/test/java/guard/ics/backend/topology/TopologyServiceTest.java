package guard.ics.backend.topology;

import guard.ics.backend.common.dto.PageDTO;
import guard.ics.backend.common.exception.ResourceNotFoundException;
import guard.ics.backend.topology.dto.TopologyEventResponse;
import guard.ics.backend.topology.entity.TopologyEventEntity;
import guard.ics.backend.topology.repository.TopologyEventRepository;
import guard.ics.backend.topology.service.TopologyService;
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
class TopologyServiceTest {

    @Mock
    private TopologyEventRepository topologyEventRepository;

    private TopologyService topologyService;

    @BeforeEach
    void setUp() {
        topologyService = new TopologyService(topologyEventRepository);
    }

    // ── listEvents ────────────────────────────────────────────────

    @Test
    void shouldListTopologyEventsWithAllFilters() {
        TopologyEventEntity entity = createEvent(1L, "device_up", "d1", "online");
        Page<TopologyEventEntity> page = new PageImpl<>(List.of(entity));
        when(topologyEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TopologyEventResponse> result = topologyService.listEvents(
                "device_up", "d1", "online",
                Instant.now().minusSeconds(3600), Instant.now(), PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        TopologyEventResponse r = result.content().get(0);
        assertThat(r.eventType()).isEqualTo("device_up");
        assertThat(r.deviceId()).isEqualTo("d1");
        assertThat(r.deviceName()).isEqualTo("PLC-1");
        assertThat(r.deviceType()).isEqualTo("PLC");
        assertThat(r.status()).isEqualTo("online");
        assertThat(r.ipAddress()).isEqualTo("10.0.0.1");
    }

    @Test
    void shouldListTopologyEventsWithoutFilters() {
        TopologyEventEntity e1 = createEvent(1L, "device_up", "d1", "online");
        TopologyEventEntity e2 = createEvent(2L, "device_offline", "d2", "offline");
        Page<TopologyEventEntity> page = new PageImpl<>(List.of(e1, e2));
        when(topologyEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TopologyEventResponse> result = topologyService.listEvents(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).hasSize(2);
    }

    @Test
    void shouldReturnEmptyTopologyEventPage() {
        Page<TopologyEventEntity> empty = Page.empty();
        when(topologyEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        PageDTO<TopologyEventResponse> result = topologyService.listEvents(
                null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    @Test
    void shouldListTopologyEventsWithPartialFilters() {
        TopologyEventEntity entity = createEvent(1L, "topology_change", "d3", "online");
        Page<TopologyEventEntity> page = new PageImpl<>(List.of(entity));
        when(topologyEventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageDTO<TopologyEventResponse> result = topologyService.listEvents(
                "topology_change", null, "online", null, null, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).eventType()).isEqualTo("topology_change");
        assertThat(result.content().get(0).status()).isEqualTo("online");
    }

    // ── getEventById ──────────────────────────────────────────────

    @Test
    void shouldGetTopologyEventById() {
        TopologyEventEntity entity = createEvent(1L, "device_up", "d1", "online");
        when(topologyEventRepository.findById(1L)).thenReturn(Optional.of(entity));

        TopologyEventResponse result = topologyService.getEventById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.traceId()).isEqualTo("trace-1");
        assertThat(result.eventType()).isEqualTo("device_up");
        assertThat(result.deviceId()).isEqualTo("d1");
        assertThat(result.deviceName()).isEqualTo("PLC-1");
        assertThat(result.deviceType()).isEqualTo("PLC");
        assertThat(result.ipAddress()).isEqualTo("10.0.0.1");
        assertThat(result.macAddress()).isEqualTo("aa:bb:cc:dd:ee:01");
        assertThat(result.port()).isEqualTo("eth0");
        assertThat(result.status()).isEqualTo("online");
        assertThat(result.occurredAt()).isNotNull();
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void shouldThrowNotFoundWhenTopologyEventMissing() {
        when(topologyEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> topologyService.getEventById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getLatestDevices ──────────────────────────────────────────

    @Test
    void shouldReturnLatestDevices() {
        TopologyEventEntity e1 = createEvent(1L, "device_up", "d1", "online");
        TopologyEventEntity e2 = createEvent(2L, "device_up", "d2", "online");
        e2.setDeviceName("RTU-1");
        e2.setDeviceType("RTU");
        e2.setIpAddress("10.0.0.2");
        when(topologyEventRepository.findLatestPerDevice()).thenReturn(List.of(e1, e2));

        List<TopologyEventResponse> result = topologyService.getLatestDevices();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).deviceId()).isEqualTo("d1");
        assertThat(result.get(1).deviceId()).isEqualTo("d2");
    }

    @Test
    void shouldReturnEmptyLatestDevicesWhenNoDevices() {
        when(topologyEventRepository.findLatestPerDevice()).thenReturn(List.of());

        List<TopologyEventResponse> result = topologyService.getLatestDevices();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleLatestDevice() {
        TopologyEventEntity entity = createEvent(1L, "device_up", "d1", "online");
        when(topologyEventRepository.findLatestPerDevice()).thenReturn(List.of(entity));

        List<TopologyEventResponse> result = topologyService.getLatestDevices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).deviceId()).isEqualTo("d1");
    }

    // ── getDeviceHistory ──────────────────────────────────────────

    @Test
    void shouldReturnDeviceHistory() {
        TopologyEventEntity e1 = createEvent(1L, "device_up", "d1", "online");
        TopologyEventEntity e2 = createEvent(2L, "topology_change", "d1", "online");
        e2.setOccurredAt(Instant.now().plusSeconds(60));
        when(topologyEventRepository.findByDeviceIdOrderByOccurredAtDesc("d1"))
                .thenReturn(List.of(e2, e1));

        List<TopologyEventResponse> result = topologyService.getDeviceHistory("d1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).eventType()).isEqualTo("topology_change");
        assertThat(result.get(1).eventType()).isEqualTo("device_up");
        verify(topologyEventRepository).findByDeviceIdOrderByOccurredAtDesc("d1");
    }

    @Test
    void shouldReturnEmptyHistoryForUnknownDevice() {
        when(topologyEventRepository.findByDeviceIdOrderByOccurredAtDesc("unknown"))
                .thenReturn(List.of());

        List<TopologyEventResponse> result = topologyService.getDeviceHistory("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnHistoryWithSingleEvent() {
        TopologyEventEntity entity = createEvent(5L, "device_offline", "d1", "offline");
        when(topologyEventRepository.findByDeviceIdOrderByOccurredAtDesc("d1"))
                .thenReturn(List.of(entity));

        List<TopologyEventResponse> result = topologyService.getDeviceHistory("d1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).eventType()).isEqualTo("device_offline");
        assertThat(result.get(0).status()).isEqualTo("offline");
    }

    // ── helpers ───────────────────────────────────────────────────

    private TopologyEventEntity createEvent(Long id, String eventType, String deviceId, String status) {
        return TopologyEventEntity.builder()
                .id(id).traceId("trace-" + id).eventType(eventType)
                .deviceId(deviceId).deviceName("PLC-1").deviceType("PLC")
                .ipAddress("10.0.0.1").macAddress("aa:bb:cc:dd:ee:01")
                .port("eth0").status(status)
                .occurredAt(Instant.now()).createdAt(Instant.now()).build();
    }
}
