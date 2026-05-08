package com.gradhire.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String message;

    private String role;

    private String createdBy;

    private String targetUser;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean readStatus = false;

    @Column(updatable = false)
    private LocalDateTime time;

    @Column(name = "action_type")
    private String actionType;

    @PrePersist
    protected void onCreate() {
        time = LocalDateTime.now();
    }
}
