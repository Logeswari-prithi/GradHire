package com.gradhire.repository;

import com.gradhire.entity.ResumeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResumeHistoryRepository extends JpaRepository<ResumeHistory, Long> {
    void deleteByExpirationDateBefore(LocalDateTime currentDate);
    List<ResumeHistory> findByExpirationDateBefore(LocalDateTime currentDate);
    List<ResumeHistory> findAllByOrderByCreatedAtDesc();
}
