package timesheets.integration.issue;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JiraIssueResponse {
    private String key;
    private String summary; 
    private String status;
}