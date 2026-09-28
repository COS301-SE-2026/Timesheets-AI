package timesheets.integration.calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final SecurityUtils securityUtils;
  private final IntegrationTokenRepository integrationTokenRepository;
  private final TimeEntryRepository timeEntryRepository;

  private static final double MINUTES_PER_HOUR = 60.0;
  private static final double SECONDS_PER_HOUR = 3600.0;
  private static final double ROUNDING_FACTOR = 100.0;

  private static double round2(double value) {
    return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
  }

  public record CalendarVsTrackedResponse(
      boolean connected, double calendarHours, double trackedHours, double unmatchedHours) {}

  // returns the calendar statuts
  @GetMapping("/status")
  public ResponseEntity<CalendarStatus> getStatus() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    return integrationTokenRepository
        .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "GOOGLE_CALENDAR")
        .map(token -> ResponseEntity.ok(new CalendarStatus(true, "google", null)))
        .orElseGet(() -> ResponseEntity.ok(new CalendarStatus(false, null, null)));
  }

  @GetMapping("/events")
  public ResponseEntity<List<CalendarEvent>> getEvents(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
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

  @GetMapping("/vs-tracked")
  public ResponseEntity<CalendarVsTrackedResponse> getVsTracked(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    // bad range would silently return zeros otherwise
    if (from.isAfter(to)) {
      return ResponseEntity.badRequest().build();
    }

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    LocalDateTime start = from.atStartOfDay();
    LocalDateTime end = to.atTime(23, 59, 59);

    List<CalendarEvent> events;
    try {
      events = calendarService.getEvents(workspaceMemberId, start, end);
    } catch (CalendarNotConnectedException e) {
      // normal state, user just has not connected yet
      return ResponseEntity.ok(new CalendarVsTrackedResponse(false, 0, 0, 0));
    } catch (RuntimeException e) {
      // real failure, log it so it does not hide behind "not connected"
      log.error("calendar vs-tracked failed for member {}", workspaceMemberId, e);
      return ResponseEntity.ok(new CalendarVsTrackedResponse(false, 0, 0, 0));
    }

    double calendarHours =
        events.stream()
            .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
            .mapToDouble(
                e ->
                    Duration.between(e.getStartTime(), e.getEndTime()).toMinutes()
                        / MINUTES_PER_HOUR)
            .sum();

    double trackedHours =
        timeEntryRepository
            .findByWorkspaceMemberIdAndStartTimeBetween(workspaceMemberId, start, end)
            .stream()
            .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
            .mapToDouble(
                t ->
                    (t.getDurationSeconds() != null ? t.getDurationSeconds() : 0)
                        / SECONDS_PER_HOUR)
            .sum();

    double unmatchedHours = Math.max(calendarHours - trackedHours, 0);

    return ResponseEntity.ok(
        new CalendarVsTrackedResponse(
            true, round2(calendarHours), round2(trackedHours), round2(unmatchedHours)));
  }
}
