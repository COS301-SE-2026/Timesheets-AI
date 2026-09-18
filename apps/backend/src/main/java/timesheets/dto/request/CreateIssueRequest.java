package timesheets.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateIssueRequest {
  @NotBlank(message = "Title is required")
  private String title;

  private String description;

  @NotBlank(message = "Project key is required")
  private String projectKey;

  @NotBlank(message = "Issue type is required")
  private String issueType;

  private String priority;
  private String dueDate;
  private String assigneeEmail;
}
