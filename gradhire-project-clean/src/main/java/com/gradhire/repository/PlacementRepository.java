package com.gradhire.repository;

import com.gradhire.entity.Placement;
import com.gradhire.entity.PlacementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlacementRepository extends JpaRepository<Placement, Long> {
    List<Placement> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Placement> findByStudentId(Long studentId);
    List<Placement> findByOverallStatus(PlacementStatus status);
    List<Placement> findAllByOrderByCreatedAtDesc();
    
    long countByDriveId(Long driveId);
    List<Placement> findByDriveId(Long driveId);

    @Query("SELECT COUNT(p) FROM Placement p WHERE p.overallStatus = com.gradhire.entity.PlacementStatus.SELECTED")
    long countSelectedPlacements();

    @Query("SELECT p.companyName, COUNT(p) FROM Placement p GROUP BY p.companyName ORDER BY COUNT(p) DESC")
    List<Object[]> getPlacementsByCompany();

    @Query("SELECT p FROM Placement p WHERE p.student.batch.id = :batchId ORDER BY p.createdAt DESC")
    List<Placement> findByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT p FROM Placement p WHERE " +
           "LOWER(p.student.user.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.student.registerNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.companyName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.jobRole) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.student.department) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(CAST(p.overallStatus AS string)) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY COALESCE(p.interviewDate, p.createdAt) DESC")
    List<Placement> searchPlacements(@Param("q") String q);

    List<Placement> findAllByOrderByInterviewDateDesc();
}
