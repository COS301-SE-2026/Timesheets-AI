package timesheets.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import timesheets.domain.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // gets all the notifications for a workspace member, with the newest first
  List<Notification> findByWorkspaceMemberIdOrderByCreatedAtDesc(UUID workspaceMemberId);

  // gets all the unread notifications, with the newest first
  List<Notification> findByWorkspaceMemberIdAndIsReadFalseOrderByCreatedAtDesc(
      UUID workspaceMemberId);

  // used when marking all notifications as read
  List<Notification> findByWorkspaceMemberIdAndIsReadFalse(UUID workspaceMemberId);

  // counts the unread notifications for the notifications badge
  long countByWorkspaceMemberIdAndIsReadFalse(UUID workspaceMemberId);
}
