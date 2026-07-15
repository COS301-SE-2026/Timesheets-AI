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
public class StopTimerResponse {

  private UUID timerId;
  private LocalDateTime stoppedAt;
  private Integer durationMinutes;
  private CreatedTimeEntryResponse createdTimeEntry;
}
