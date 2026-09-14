package timesheets.integration.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalendarService {
  private final CalendarAdapter calendarAdapter;

  public List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    return calendarAdapter.getEvents(workspaceMemberId, startTime, endTime);
  }

  public CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId) {
    return calendarAdapter.getEvent(workspaceMemberId, externalEventId);
  }
}
