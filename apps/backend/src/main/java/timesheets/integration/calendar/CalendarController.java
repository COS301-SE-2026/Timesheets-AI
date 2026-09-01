package timesheets.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.security.SecurityUtils;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final SecurityUtils securityUtils;

  @GetMapping("/events")
  public ResponseEntity<List<CalendarEvent>> getEvents(
      @RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    List<CalendarEvent> events = calendarService.getEvents(workspaceMemberId, startTime, endTime);
    return ResponseEntity.ok(events);
  }
}
