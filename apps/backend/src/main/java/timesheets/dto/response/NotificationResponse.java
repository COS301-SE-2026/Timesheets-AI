package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.domain.Notification;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

  private UUID id;
  private String type;
  private String title;
  private String message;
  private String entityType;
  private UUID entityId;
  private Boolean isRead;
  private LocalDateTime createdAt;

  public static NotificationResponse from(Notification notification) {
    return NotificationResponse.builder()
        .id(notification.getId())
        .type(notification.getType())
        .title(notification.getTitle())
        .message(notification.getMessage())
        .entityType(notification.getEntityType())
        .entityId(notification.getEntityId())
        .isRead(notification.getIsRead())
        .createdAt(notification.getCreatedAt())
        .build();
  }
}
