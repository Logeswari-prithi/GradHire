package com.gradhire.repository;

import com.gradhire.entity.PlacementDrive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {
    
    @Query("SELECT d FROM PlacementDrive d WHERE d.applicationDeadline >= :currentDate ORDER BY d.driveDate ASC")
    List<PlacementDrive> findOngoingDrives(LocalDate currentDate);
    
    @Query("SELECT d FROM PlacementDrive d WHERE d.applicationDeadline < :currentDate ORDER BY d.driveDate DESC")
    List<PlacementDrive> findPastDrives(LocalDate currentDate);
}
