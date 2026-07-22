package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimerStatusResponse {
  private Boolean hasActiveTimer;
  private Boolean isRunning;
  private Boolean isPaused;
  private UUID projectId;
  private UUID timerId;
  private UUID taskId;
  private LocalDateTime startedAt;
  private LocalDateTime pausedAt;
  private Long pausedDurationSeconds;
}
