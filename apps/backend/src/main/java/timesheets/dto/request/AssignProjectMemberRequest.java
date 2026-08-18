package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AssignProjectMemberRequest {

  @NotNull(message = "Workspace member ID is required")
  private UUID workspaceMemberId;

  private Boolean isProjectManager = false; // a project manager or just a developer

  // want this to be set from the path variable in the controller so that the URL and request body
  // match
  private UUID projectId;
}
