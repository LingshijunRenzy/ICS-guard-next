package guard.ics.backend.dashboard;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.alert.service.AlertService;
import guard.ics.backend.dashboard.dto.DashboardResponse;
import guard.ics.backend.dashboard.service.DashboardService;
import guard.ics.backend.metric.dto.TrafficStatsResponse;
import guard.ics.backend.metric.service.TrafficMetricService;
import guard.ics.backend.rule.repository.RuleRepository;
import guard.ics.backend.topology.dto.TopologyEventResponse;
import guard.ics.backend.topology.service.TopologyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AlertService alertService;
    @Mock
    private TopologyService topologyService;
    @Mock
    private TrafficMetricService trafficMetricService;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private RuleRepository ruleRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnAggregatedDashboard() {
        when(alertService.stats(null)).thenReturn(
                new AlertStatsResponse(Map.of("high", 3L, "medium", 5L),
                        Map.of("new", 6L, "resolved", 2L), 8L));

        when(topologyService.getLatestDevices()).thenReturn(List.of(
                new TopologyEventResponse(1L, "t1", "device_up", "d1", "Device1",
                        "PLC", "10.0.0.1", null, null, "online", Instant.now(), Instant.now()),
                new TopologyEventResponse(2L, "t2", "device_up", "d2", "Device2",
                        "RTU", "10.0.0.2", null, null, "online", Instant.now(), Instant.now())));

        when(ruleRepository.countByEnabledTrue()).thenReturn(3L);
        when(ruleRepository.count()).thenReturn(5L);

        AlertEntity recent = new AlertEntity();
        recent.setId(1L); recent.setTraceId("t1"); recent.setAlertType("anomaly");
        recent.setSeverity("high"); recent.setStatus("new");
        recent.setSourceIp("10.0.0.1"); recent.setDestIp("10.0.0.2");
        recent.setProtocol("TCP"); recent.setTriggeredAt(Instant.now()); recent.setCreatedAt(Instant.now());
        when(alertRepository.findTop10ByOrderByTriggeredAtDesc()).thenReturn(List.of(recent));

        TrafficStatsResponse trafficStats = new TrafficStatsResponse(
                List.of(new TrafficStatsResponse.TopEntry("10.0.0.1", 5000L)),
                List.of(new TrafficStatsResponse.TopEntry("10.0.0.2", 3000L)),
                List.of());
        when(trafficMetricService.stats(null)).thenReturn(trafficStats);

        DashboardResponse result = dashboardService.aggregate();

        assertThat(result.totalAlerts()).isEqualTo(8L);
        assertThat(result.totalDevices()).isEqualTo(2L);
        assertThat(result.activeRuleCount()).isEqualTo(3L);
        assertThat(result.totalRuleCount()).isEqualTo(5L);
        assertThat(result.recentAlerts()).hasSize(1);
        assertThat(result.trafficSummary().topSources()).hasSize(1);
        assertThat(result.trafficSummary().topDestinations()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyDashboard() {
        when(alertService.stats(null)).thenReturn(
                new AlertStatsResponse(Map.of(), Map.of(), 0L));
        when(topologyService.getLatestDevices()).thenReturn(List.of());
        when(ruleRepository.countByEnabledTrue()).thenReturn(0L);
        when(ruleRepository.count()).thenReturn(0L);
        when(alertRepository.findTop10ByOrderByTriggeredAtDesc()).thenReturn(List.of());
        when(trafficMetricService.stats(null)).thenReturn(
                new TrafficStatsResponse(List.of(), List.of(), List.of()));

        DashboardResponse result = dashboardService.aggregate();

        assertThat(result.totalAlerts()).isEqualTo(0L);
        assertThat(result.totalDevices()).isEqualTo(0L);
        assertThat(result.recentAlerts()).isEmpty();
    }
}
