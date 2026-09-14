package timesheets.service;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Notification;
import timesheets.dto.response.NotificationResponse;
import timesheets.repository.NotificationRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final SecurityUtils securityUtils;

  // this will make a notification for a workspace member
  public Notification createNotification(
      UUID workspaceMemberId,
      String type,
      String title,
      String message,
      String entityType,
      UUID entityId) {

    Notification notification =
        Notification.builder()
            .workspaceMemberId(workspaceMemberId)
            .type(type)
            .title(title)
            .message(message)
            .entityType(entityType)
            .entityId(entityId)
            .isRead(false)
            .build();
    return notificationRepository.save(notification);
  }

  // gets all notifications for the current workspace member
  public List<NotificationResponse> getMyNotifications() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return notificationRepository
        .findByWorkspaceMemberIdOrderByCreatedAtDesc(workspaceMemberId)
        .stream()
        .map(NotificationResponse::from)
        .toList();
  }

  // this gets the unread notifications
  public List<NotificationResponse> getMyUnreadNotifications() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return notificationRepository
        .findByWorkspaceMemberIdAndIsReadFalseOrderByCreatedAtDesc(workspaceMemberId)
        .stream()
        .map(NotificationResponse::from)
        .toList();
  }

  // gets the unread notification for the notification badge
  public long getMyUnreadCount() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return notificationRepository.countByWorkspaceMemberIdAndIsReadFalse(workspaceMemberId);
  }

  // marks a notification as read
  @Transactional
  public NotificationResponse markAsRead(UUID notificationId) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    Notification notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

    if (!workspaceMemberId.equals(notification.getWorkspaceMemberId())) {
      throw new AccessDeniedException("You do not have access to this notification");
    }

    notification.setIsRead(true);

    Notification savedNotification = notificationRepository.save(notification);
    return NotificationResponse.from(savedNotification);
  }

  // marks all the unread notications for the current workspace member as read
  @Transactional
  public void markAllAsRead() {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<Notification> notifications =
        notificationRepository.findByWorkspaceMemberIdAndIsReadFalse(workspaceMemberId);

    notifications.forEach(notification -> notification.setIsRead(true));
    notificationRepository.saveAll(notifications);
  }
}
