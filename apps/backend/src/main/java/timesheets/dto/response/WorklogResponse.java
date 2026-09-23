// this file is about Jira worklog evidence  - which gives us the actual time-tracking activty from
// Jira
// worklog is a record of time spent on an issue

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
public class WorklogResponse {
  private String issueKey;
  private String worklogId;
  private String authorDisplayName;
  private String authorEmail;
  private LocalDateTime startedAt;
  private int timeSpentSeconds;
  private String description;
}
