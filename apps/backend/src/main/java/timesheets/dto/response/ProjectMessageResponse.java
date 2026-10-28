package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import timesheets.enums.ReactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMessageResponse {

  private UUID id;
  private UUID projectId;
  private UUID authorWorkspaceMemberId;
  private String authorName;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // true when the logged-in user wrote this message
  private boolean mine;

  private List<ReactionSummary> reactions;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReactionSummary {
    private ReactionType type;
    private int count;
    private boolean reactedByMe;
  }
}
