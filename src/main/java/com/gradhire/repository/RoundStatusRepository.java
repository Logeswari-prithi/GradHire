package com.gradhire.repository;

import com.gradhire.entity.RoundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoundStatusRepository extends JpaRepository<RoundStatus, Long> {
    List<RoundStatus> findByPlacementId(Long placementId);
}
