package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

public class AccountDeletionResponse {

  // this will be the response for the pending deletions to be shown to the admins
  @Data
  @Builder
  public static class PendingRequest {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDateTime deletionRequestedAt;
    private String deletionReason;
    private LocalDateTime userCreatedAt;
    private Integer daysPending;
    private Long workspaceCount;
    private String status;
  }

  // this is the response after a deletion has been processed
  @Data
  @Builder
  public static class ProcessedResult {
    private UUID userId;
    private String email;
    private String action;
    private LocalDateTime processedAt;
    private String message;
  }

  // deletion status response for the user
  @Data
  @Builder
  public static class DeletionStatus {
    private boolean hasPendingRequest;
    private LocalDateTime requestedAt;
    private String reason;
    private boolean isProcessed;
    private LocalDateTime processedAt;
    private String status;
  }
}
