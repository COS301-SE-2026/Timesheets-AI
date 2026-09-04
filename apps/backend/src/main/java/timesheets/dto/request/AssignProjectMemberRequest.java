package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AssignProjectMemberRequest {

  @NotNull(message = "Workspace member ID is required")
  private UUID workspaceMemberId;

  private Boolean isProjectManager = false; // a project manager or just a developer
}
