package com.gradhire.repository;

import com.gradhire.entity.PlacementActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacementActionLogRepository extends JpaRepository<PlacementActionLog, Long> {
    List<PlacementActionLog> findByPlacementIdOrderByTimestampDesc(Long placementId);
    List<PlacementActionLog> findByStudentIdOrderByTimestampDesc(Long studentId);
}
