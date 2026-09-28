package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MicrosoftAuthRequest {

  @NotBlank(message = "Microsoft ID token is required")
  private String idToken;
}
