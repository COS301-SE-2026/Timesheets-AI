package timesheets.integration.calender;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// CalenderAdapter uses CalenderEvent
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalenderEvent {
  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String externalEventId;
}
