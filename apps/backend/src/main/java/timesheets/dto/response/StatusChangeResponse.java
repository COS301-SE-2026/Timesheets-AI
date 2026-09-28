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
public class StatusChangeResponse {
  private String issueKey;
  private String changeLogId;
  private String authorDisplayName;
  private String authorEmail;
  private String fromStatus;
  private String toStatus;
  private LocalDateTime changedAt;
}
