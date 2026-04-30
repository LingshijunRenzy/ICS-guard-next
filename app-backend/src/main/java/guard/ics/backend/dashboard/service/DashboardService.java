package guard.ics.backend.dashboard.service;

import guard.ics.backend.alert.dto.AlertResponse;
import guard.ics.backend.alert.dto.AlertStatsResponse;
import guard.ics.backend.alert.entity.AlertEntity;
import guard.ics.backend.alert.repository.AlertRepository;
import guard.ics.backend.alert.service.AlertService;
import guard.ics.backend.dashboard.dto.DashboardResponse;
import guard.ics.backend.metric.dto.TrafficStatsResponse;
import guard.ics.backend.metric.service.TrafficMetricService;
import guard.ics.backend.rule.repository.RuleRepository;
import guard.ics.backend.topology.service.TopologyService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DashboardService {

    private final AlertService alertService;
    private final TopologyService topologyService;
    private final TrafficMetricService trafficMetricService;
    private final AlertRepository alertRepository;
    private final RuleRepository ruleRepository;

    public DashboardService(AlertService alertService, TopologyService topologyService,
                            TrafficMetricService trafficMetricService,
                            AlertRepository alertRepository, RuleRepository ruleRepository) {
        this.alertService = alertService;
        this.topologyService = topologyService;
        this.trafficMetricService = trafficMetricService;
        this.alertRepository = alertRepository;
        this.ruleRepository = ruleRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "'aggregate'")
    public DashboardResponse aggregate() {
        AlertStatsResponse alertStats = alertService.stats(null);

        long totalDevices = topologyService.getLatestDevices().size();

        long activeRuleCount = ruleRepository.countByEnabledTrue();
        long totalRuleCount = ruleRepository.count();

        List<AlertResponse> recentAlerts = alertRepository.findTop10ByOrderByTriggeredAtDesc().stream()
                .map(AlertResponse::from).toList();

        TrafficStatsResponse trafficStats = trafficMetricService.stats(null);
        List<DashboardResponse.TopEntry> topSources = trafficStats.topSources().stream()
                .map(e -> new DashboardResponse.TopEntry(e.ip(), e.totalBytes())).toList();
        List<DashboardResponse.TopEntry> topDestinations = trafficStats.topDestinations().stream()
                .map(e -> new DashboardResponse.TopEntry(e.ip(), e.totalBytes())).toList();
        DashboardResponse.TrafficSummary trafficSummary = new DashboardResponse.TrafficSummary(topSources, topDestinations);

        return new DashboardResponse(
                alertStats.total(), alertStats.bySeverity(), alertStats.byStatus(),
                totalDevices, activeRuleCount, totalRuleCount, recentAlerts, trafficSummary);
    }
}
