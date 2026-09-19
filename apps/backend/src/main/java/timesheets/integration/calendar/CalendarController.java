package timesheets.integration.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.security.SecurityUtils;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final SecurityUtils securityUtils;
  private final IntegrationTokenRepository intergrationTokenRepository;

  // returns the calendar statuts
  @GetMapping("/status")
  public ResponseEntity<CalendarStatus> getStatus() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return intergrationTokenRepository
        .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "GOOGLE_CALENDAR")
        .map(token -> ResponseEntity.ok(new CalendarStatus(true, "google", null)))
        .orElseGet(() -> ResponseEntity.ok(new CalendarStatus(false, null, null)));
  }

  @GetMapping("/events")
  public ResponseEntity<List<CalendarEvent>> getEvents(
      @RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    List<CalendarEvent> events = calendarService.getEvents(workspaceMemberId, startTime, endTime);
    return ResponseEntity.ok(events);
  }

  @GetMapping("/events/{externalEventId}")
  public ResponseEntity<CalendarEvent> getEvent(@PathVariable String externalEventId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    CalendarEvent event = calendarService.getEvent(workspaceMemberId, externalEventId);

    return ResponseEntity.ok(event);
  }
}
