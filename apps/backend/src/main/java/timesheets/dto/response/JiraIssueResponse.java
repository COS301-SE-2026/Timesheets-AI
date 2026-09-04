package timesheets.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueResponse {

  private String key;
  private String summary;
  private String status;
  private String issueType;

  private String description;
  private String priority;
  private String assigneeEmail;
  private String assigneeDisplayName;
  private String projectKey;
  private String projectName;
  private String createdAt;
  private String updatedAt;
  private String dueDate;

  private UUID localTaskId;
}
