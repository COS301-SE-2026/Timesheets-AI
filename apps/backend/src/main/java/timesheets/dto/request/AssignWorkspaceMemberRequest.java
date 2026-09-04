package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import timesheets.enums.WorkspaceRole;

@Data
public class AssignWorkspaceMemberRequest {
  @NotNull(message = "Workspace member ID is required")
  private UUID userId;

  @NotNull(message = "Workspace ID is required")
  private UUID workspaceId;

  // an admin can assign other admins, or managers or just a dev
  @NotNull(message = "Role is required")
  private WorkspaceRole role;
}
