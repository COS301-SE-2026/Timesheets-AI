package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendProjectMessageRequest {

  @NotBlank(message = "Message cannot be empty")
  @Size(max = 2000, message = "Message must be 2000 characters or fewer")
  private String content;
}
