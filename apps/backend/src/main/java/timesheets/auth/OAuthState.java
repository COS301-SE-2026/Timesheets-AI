package timesheets.integration.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// this is the data object - this what we get after decoding and verifying OAuth state

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuthState {

  private UUID workspaceMemberId;
  private String provider;
}
