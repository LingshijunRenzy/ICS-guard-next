package guard.ics.backend.topology.repository;

import guard.ics.backend.topology.entity.TopologyEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TopologyEventRepository extends JpaRepository<TopologyEventEntity, Long>, JpaSpecificationExecutor<TopologyEventEntity> {

    @Query(value = "SELECT DISTINCT ON (device_id) * FROM topology_events ORDER BY device_id, occurred_at DESC", nativeQuery = true)
    List<TopologyEventEntity> findLatestPerDevice();

    List<TopologyEventEntity> findByDeviceIdOrderByOccurredAtDesc(String deviceId);
}
