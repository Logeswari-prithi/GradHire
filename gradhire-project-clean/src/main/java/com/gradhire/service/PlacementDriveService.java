package com.gradhire.service;

import com.gradhire.entity.PlacementDrive;
import com.gradhire.exception.ResourceNotFoundException;
import com.gradhire.repository.PlacementDriveRepository;
import com.gradhire.repository.PlacementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlacementDriveService {

    private final PlacementDriveRepository placementDriveRepository;
    private final PlacementRepository placementRepository;

    @Transactional
    public PlacementDrive createDrive(PlacementDrive drive) {
        return placementDriveRepository.save(drive);
    }

    public List<PlacementDrive> getAllDrives() {
        List<PlacementDrive> drives = placementDriveRepository.findAll();
        drives.forEach(d -> d.setStudentsAppliedCount(placementRepository.countByDriveId(d.getId())));
        return drives;
    }

    public List<PlacementDrive> getOngoingDrives() {
        List<PlacementDrive> drives = placementDriveRepository.findOngoingDrives(LocalDate.now());
        drives.forEach(d -> d.setStudentsAppliedCount(placementRepository.countByDriveId(d.getId())));
        return drives;
    }

    public List<PlacementDrive> getPastDrives() {
        List<PlacementDrive> drives = placementDriveRepository.findPastDrives(LocalDate.now());
        drives.forEach(d -> d.setStudentsAppliedCount(placementRepository.countByDriveId(d.getId())));
        return drives;
    }

    @Transactional
    public PlacementDrive updateDrive(Long id, PlacementDrive updatedDrive) {
        PlacementDrive d = placementDriveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Drive not found"));
        if (updatedDrive.getCompanyName() != null) d.setCompanyName(updatedDrive.getCompanyName());
        if (updatedDrive.getJobRole() != null) d.setJobRole(updatedDrive.getJobRole());
        if (updatedDrive.getPackageOffered() != null) d.setPackageOffered(updatedDrive.getPackageOffered());
        if (updatedDrive.getDriveDate() != null) d.setDriveDate(updatedDrive.getDriveDate());
        if (updatedDrive.getApplicationDeadline() != null) d.setApplicationDeadline(updatedDrive.getApplicationDeadline());
        return placementDriveRepository.save(d);
    }

    @Transactional
    public void deleteDrive(Long id) {
        List<com.gradhire.entity.Placement> placements = placementRepository.findByDriveId(id);
        for(com.gradhire.entity.Placement p : placements) {
            p.setDrive(null);
            placementRepository.save(p);
        }
        placementDriveRepository.deleteById(id);
    }
}
