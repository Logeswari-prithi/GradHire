package com.gradhire.repository;

import com.gradhire.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReadStatusFalseOrderByTimeDesc();
    List<Notification> findAllByOrderByTimeDesc();
    long countByReadStatusFalse();
    List<Notification> findByTargetUserOrderByTimeDesc(String targetUser);
    List<Notification> findByTargetUserIsNullOrderByTimeDesc();
    long countByTargetUserAndReadStatusFalse(String targetUser);
}
