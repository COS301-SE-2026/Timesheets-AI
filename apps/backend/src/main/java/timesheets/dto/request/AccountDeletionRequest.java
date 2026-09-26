package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

public class AccountDeletionRequest {

  // this is the user request for account deletion
  @Data
  public static class Request {

    @NotBlank(message = "Reason for deletion is required")
    private String reason;
  }

  // this will be for an admin to process a deletion request
  @Data
  public static class Process {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Action is required")
    private Action action;

    private String rejectionReason;

    public enum Action {
      APPROVE,
      REJECT
    }
  }

  // this will be for when a user cancels their request
  @Data
  public static class Cancel {
    // no fields yet since the user id is in the Security Context
  }
}
