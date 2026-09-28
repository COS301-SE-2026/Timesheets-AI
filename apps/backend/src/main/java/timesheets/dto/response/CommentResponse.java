package timesheets.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
  private String issueKey;
  private String commentId;
  private String authorDisplayName;
  private String authorEmail;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String body;
}
