package timesheets.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.response.NotificationResponse;
import timesheets.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  /*
  - gets all the notifications for the current workspace member
  - notifications are returned with the newest notifications first 
  */
  @GetMapping
  public ResponseEntity<List<NotificationResponse>> getMyNotifications() {

    return ResponseEntity.ok(notificationService.getMyNotifications());
  }

  /*
  - gets the unread notifications for the current workspace member
  - this is used for the frontend to display unread notifications
   */
  @GetMapping("/unread")
  public ResponseEntity<List<NotificationResponse>> getMyUnreadNotifications() {

    return ResponseEntity.ok(notificationService.getMyUnreadNotifications());
  }

  //gets the unread notifications for a current workspace member, can be used with the badge
  @GetMapping("/unread/count")
  public ResponseEntity<Long> getMyUnreadCount() {

    return ResponseEntity.ok(notificationService.getMyUnreadCount());
  }

  /*
  - this will mark a specific notification as read
  - the notification id is passed through the URL
  */
  @PatchMapping("/{notificationId}/read")
  public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID notificationId) {

    return ResponseEntity.ok(notificationService.markAsRead(notificationId));
  }

  /*
  - this will mark all the notifications for the current workspace members as read
  - since no response body is needed it will just return 204
   */
  @PatchMapping("/read-all")
  public ResponseEntity<Void> markAllAsRead() {

    notificationService.markAllAsRead();

    return ResponseEntity.noContent().build();
  }
}
