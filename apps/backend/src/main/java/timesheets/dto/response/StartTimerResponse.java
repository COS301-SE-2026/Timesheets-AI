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
public class StartTimerResponse {

  private UUID id;
  private UUID projectId;
  private UUID taskId;
  private LocalDateTime startedAt;
  private Boolean active;
}
