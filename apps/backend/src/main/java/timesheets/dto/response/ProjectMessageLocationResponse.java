package timesheets.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// used by notifications to find which project a message belongs to
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMessageLocationResponse {
  private UUID projectId;
  private UUID messageId;
}
