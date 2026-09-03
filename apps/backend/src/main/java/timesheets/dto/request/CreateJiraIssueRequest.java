package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateJiraIssueRequest {
  @NotBlank(message = "Summary is required")
  private String summary;

  private String description;

  @NotBlank(message = "Project key is required")
  private String projectKey;

  @NotBlank(message = "Issue type is required")
  private String issueType;

  private String priority;
  private String dueDate;
  private String assigneeEmail;
}
