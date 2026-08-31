package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {
  private UUID projectMemberId;
  private UUID workspaceMemberId;
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private Boolean isProjectManager;
  private LocalDateTime joinedAt;
}
