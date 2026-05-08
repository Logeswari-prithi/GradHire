package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_action_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementActionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long placementId;
    private Long studentId;
    private String actionType; // CREATE, UPDATE, DELETE
    private String entityType; // PLACEMENT, ROUND
    
    @Column(length = 2000)
    private String details;

    private String performedBy;
    
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
