package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import timesheets.enums.WorkspaceRole;

@Data
public class AssignMemberRequest {
  @NotNull(message = "Workspace member ID is required")
  private UUID workspaceMemberId;

  @NotNull(message = "Role is required")
  private WorkspaceRole role; // MANAGER or DEVELOPER
}
