package com.gradhire.repository;

import com.gradhire.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRegisterNumber(String registerNumber);
    boolean existsByRegisterNumber(String registerNumber);
    List<Student> findByBatchId(Long batchId);
    Optional<Student> findByUserId(Long userId);
    List<Student> findByUploadHistoryId(Long uploadHistoryId);

    @Query("SELECT s FROM Student s WHERE " +
           "LOWER(s.user.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.registerNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.department) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.user.phone) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(s.user.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "CAST(s.batch.year AS string) LIKE CONCAT('%', :q, '%')")
    List<Student> searchStudents(@Param("q") String q);

    @Query("SELECT s FROM Student s WHERE s.department = :dept")
    List<Student> findByDepartment(@Param("dept") String department);

    @Query("SELECT DISTINCT s.department FROM Student s WHERE s.department IS NOT NULL AND s.department <> ''")
    List<String> findDistinctDepartments();

    long countByPlacementStatus(String status);

    long countByPlacementStatusNot(String status);

    @Query("SELECT s.placementStatus as label, COUNT(s) as value FROM Student s GROUP BY s.placementStatus")
    List<java.util.Map<String, Object>> getPlacementStatusDistribution();

    @Query("SELECT s.batch.year as label, COUNT(s) as value FROM Student s GROUP BY s.batch.year")
    List<java.util.Map<String, Object>> getBatchWiseDistribution();

    @Query("SELECT s.department as label, COUNT(s) as value FROM Student s GROUP BY s.department")
    List<java.util.Map<String, Object>> getDepartmentDistribution();
}
