package com.gradhire.service;

import com.gradhire.entity.*;
import com.gradhire.exception.ResourceNotFoundException;
import com.gradhire.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ErrorReportRepository errorReportRepository;
    private final UserRepository userRepository;

    public Notification createNotification(Long senderId, String title, String message, String type) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));
        String combinedMessage = title + ": " + message;
        return notificationRepository.save(Notification.builder()
                .createdBy(sender.getUsername()).role(sender.getRole().name())
                .message(combinedMessage).actionType(type).readStatus(false).targetUser(null).build());
    }

    public Notification createNotificationByUsername(String username, String title, String message, String type) {
        User sender = userRepository.findByUsername(username).orElse(null);
        String role = sender != null ? sender.getRole().name() : "SYSTEM";
        String createdByStr = sender != null ? sender.getUsername() : username;
        String combinedMessage = title + ": " + message;
        return notificationRepository.save(Notification.builder()
                .createdBy(createdByStr).role(role).message(combinedMessage).actionType(type).readStatus(false).targetUser(null).build());
    }

    public Notification createNotificationTargeted(String createdBy, String targetUser, String title, String message, String type) {
        String combinedMessage = title + ": " + message;
        return notificationRepository.save(Notification.builder()
                .createdBy(createdBy).role("SYSTEM")
                .message(combinedMessage).actionType(type).readStatus(false).targetUser(targetUser).build());
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findByTargetUserIsNullOrderByTimeDesc();
    }

    public List<Notification> getGlobalAndUserNotifications(String username) {
        List<Notification> global = notificationRepository.findByTargetUserIsNullOrderByTimeDesc();
        List<Notification> user = notificationRepository.findByTargetUserOrderByTimeDesc(username);
        List<Notification> combined = new java.util.ArrayList<>(global);
        combined.addAll(user);
        combined.sort((n1, n2) -> n2.getTime().compareTo(n1.getTime()));
        return combined;
    }

    public List<Notification> getNotificationsForUser(String username) {
        return notificationRepository.findByTargetUserOrderByTimeDesc(username);
    }

    public void markAsRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        n.setReadStatus(true);
        notificationRepository.save(n);
    }

    public void markAllAsRead() {
        List<Notification> unreadList = notificationRepository.findByReadStatusFalseOrderByTimeDesc();
        for (Notification n : unreadList) {
            n.setReadStatus(true);
        }
        notificationRepository.saveAll(unreadList);
    }

    public void markAllAsRead(String username) {
        List<Notification> unreadList = notificationRepository.findByReadStatusFalseOrderByTimeDesc();
        List<Notification> userUnread = notificationRepository.findByTargetUserOrderByTimeDesc(username).stream().filter(n -> !n.isReadStatus()).collect(java.util.stream.Collectors.toList());
        
        for (Notification n : unreadList) n.setReadStatus(true);
        for (Notification n : userUnread) n.setReadStatus(true);
        
        notificationRepository.saveAll(unreadList);
        notificationRepository.saveAll(userUnread);
    }

    public long getUnreadCount() {
        return notificationRepository.countByReadStatusFalse();
    }

    public long getUnreadCount(String username) {
        return notificationRepository.countByReadStatusFalse() + notificationRepository.countByTargetUserAndReadStatusFalse(username);
    }

    public long getUnreadCountForUser(String username) {
        return notificationRepository.countByTargetUserAndReadStatusFalse(username);
    }

    public ErrorReport submitErrorReport(Long reporterId, String subject, String description, String severity) {
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter user not found"));
        ErrorReport saved = errorReportRepository.save(ErrorReport.builder()
                .user(reporter).subject(subject).message(description).severity(severity).status("PENDING").build());
        createNotification(reporter.getId(), "New Issue Reported", subject, "REPORT");
        return saved;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ErrorReport> getAllErrorReports() {
        return errorReportRepository.findAllWithUser();
    }

    public void markReportAsResolved(Long reportId, String adminReply, String resolvedBy) {
        ErrorReport report = errorReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setStatus("RESOLVED");
        report.setResolvedAt(java.time.LocalDateTime.now());
        report.setAdminReply(adminReply);
        report.setResolvedBy(resolvedBy);
        errorReportRepository.save(report);

        createNotificationTargeted(resolvedBy, report.getUser().getUsername(), "Report Resolved", "Your issue has been resolved: " + adminReply, "REPORT_SOLVED");
    }
}
