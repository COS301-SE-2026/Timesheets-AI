package timesheets.integration.calendar;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// CalendarAdapter uses CalendarEvent
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {
  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String externalEventId;
}
