package com.gradhire.repository;

import com.gradhire.entity.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT e FROM ErrorReport e LEFT JOIN FETCH e.user ORDER BY e.createdAt DESC")
    java.util.List<ErrorReport> findAllWithUser();
}
