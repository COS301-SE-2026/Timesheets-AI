package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.WorkspaceRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberResponse {
  private UUID workspaceMemberId;
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private WorkspaceRole role;
  private LocalDateTime joinedAt;
}
