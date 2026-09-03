package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

public class AccountDeletionRequest {

  @Data
  public static class Request {

    @NotBlank(message = "Reason for deletion is required")
    private String reason;
  }

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

  @Data
  public static class Cancel {
    // no fields yet since the user id is in the Security Context
  }
}
