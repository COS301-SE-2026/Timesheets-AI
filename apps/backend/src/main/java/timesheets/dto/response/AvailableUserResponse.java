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
public class AvailableUserResponse {
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;

  // this will help us quickly identify when someone is in the workspace
  private Boolean isInWorkspace;
}
