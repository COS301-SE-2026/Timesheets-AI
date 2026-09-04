package timesheets.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor // this will have a class with all the args
public class ErrorResponse {

  private Integer status;
  private String error;
  private String message;
  private UUID activeTimerId;

  public ErrorResponse(Integer status, String error, String message) {
    this.status = status;
    this.error = error;
    this.message = message;
    this.activeTimerId = null;
  }

  // this could be to indicate a timer conflict if the user tries to create a second timer
  public static ErrorResponse conflict(String message, UUID activeTimerId) {
    return ErrorResponse.builder()
        .status(409)
        .error("Conflict")
        .message(message)
        .activeTimerId(activeTimerId)
        .build();
  }

  // I could use this one for simple errors without extra data
  public static ErrorResponse simple(String message) {
    return ErrorResponse.builder().message(message).build();
  }
}
