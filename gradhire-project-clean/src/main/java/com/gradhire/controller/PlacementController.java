package com.gradhire.controller;

import com.gradhire.dto.*;
import com.gradhire.service.PlacementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/placements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlacementController {

    private final PlacementService placementService;

    @PostMapping
    public ResponseEntity<ApiResponse<PlacementDTO>> create(@RequestBody PlacementDTO dto, Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Placement created",
                    placementService.createPlacement(dto, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create placement: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlacementDTO>> update(@PathVariable Long id,
                                                             @RequestBody PlacementDTO dto,
                                                             Authentication auth) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Placement updated",
                    placementService.updatePlacement(id, dto, auth.getName())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update placement: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        try {
            placementService.deletePlacement(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Placement deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete placement: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlacementDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Placements retrieved", placementService.getAllPlacements()));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PlacementDTO>>> search(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("Results", placementService.searchPlacements(q)));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<ApiResponse<List<PlacementDTO>>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("Placements retrieved",
                placementService.getPlacementsByStudent(studentId)));
    }

    @GetMapping("/{id}/rounds")
    public ResponseEntity<ApiResponse<?>> getRounds(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Rounds retrieved",
                placementService.getRoundsForPlacement(id)));
    }

    @PostMapping("/{id}/rounds")
    public ResponseEntity<ApiResponse<?>> createRound(@PathVariable Long id, @RequestBody RoundStatusDTO dto, Authentication auth) {
        try {
            placementService.createRound(id, dto, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Round added", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to add round: " + e.getMessage()));
        }
    }

    @PutMapping("/rounds/{roundId}")
    public ResponseEntity<ApiResponse<Void>> updateRoundFull(@PathVariable Long roundId, @RequestBody RoundStatusDTO dto, Authentication auth) {
        try {
            placementService.updateRound(roundId, dto, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Round updated", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update round: " + e.getMessage()));
        }
    }

    @DeleteMapping("/rounds/{roundId}")
    public ResponseEntity<ApiResponse<Void>> deleteRound(@PathVariable Long roundId, Authentication auth) {
        try {
            placementService.deleteRound(roundId, auth.getName());
            return ResponseEntity.ok(ApiResponse.success("Round deleted", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete round: " + e.getMessage()));
        }
    }

    @GetMapping("/history/student/{studentId}")
    public ResponseEntity<ApiResponse<?>> getPlacementHistory(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                placementService.getPlacementHistory(studentId)));
    }
}
