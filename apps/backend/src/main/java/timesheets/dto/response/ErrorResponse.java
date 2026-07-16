package timesheets.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

  private Integer status;
  private String error;
  private String message;
  private UUID activeTimerId;

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
