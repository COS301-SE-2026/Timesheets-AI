package timesheets.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// this is a simple response object that contains a message and an optional redirect URL.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
  private String message;
  private String redirectUrl; // optional

  public MessageResponse(String message) {
    this.message = message;
  }
}
