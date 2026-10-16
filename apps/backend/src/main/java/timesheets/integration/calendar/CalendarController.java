package timesheets.integration.calendar;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import timesheets.repository.TimeEntryRepository;
import timesheets.security.SecurityUtils;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {
  private final CalendarService calendarService;
  private final SecurityUtils securityUtils;
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

    CalendarStatus status = calendarService.getStatus(workspaceMemberId);

    return ResponseEntity.ok(status);
  }

  @GetMapping("/events")
  public ResponseEntity<List<CalendarEvent>> getEvents(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

    if (!startTime.isBefore(endTime)) {
      return ResponseEntity.badRequest().build();
    }

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    /*
    Calendar integrations are optional
     Connected users retrieve their Google or Microsoft Calendar events
     Disconnected users receive an empty list instead of an unhandled exceptopns

     I need to check and fix the frontend to make it can distinguish between unconnected Calendar from a connected Calendar with no events
    */

    try {
      List<CalendarEvent> events = calendarService.getEvents(workspaceMemberId, startTime, endTime);
      return ResponseEntity.ok(events);

    } catch (CalendarNotConnectedException e) {
      return ResponseEntity.ok(List.of());
    }
  }

  @GetMapping("/events/{externalEventId}")
  public ResponseEntity<CalendarEvent> getEvent(@PathVariable String externalEventId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    try {
      CalendarEvent event = calendarService.getEvent(workspaceMemberId, externalEventId);
      return ResponseEntity.ok(event);
    } catch (CalendarNotConnectedException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  // Devs should track time without connecting to Google or Microsoft calendar
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
    LocalDateTime end = to.plusDays(1).atStartOfDay();

    // end time is to include midnight so e.g. 2026-10-16T00:00:00 to 2026-10-17T00:00:00

    // calculate tracked hours before fetching Calendar event sbecause a developer can track time
    // without connecting Google or Microsoft Calendar
    double trackedHours =
        timeEntryRepository
            .findByWorkspaceMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThan(
                workspaceMemberId, start, end)
            .stream()
            .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
            .mapToDouble(
                t ->
                    (t.getDurationSeconds() != null ? t.getDurationSeconds() : 0)
                        / SECONDS_PER_HOUR)
            .sum();

    List<CalendarEvent> events;
    try {
      events = calendarService.getEvents(workspaceMemberId, start, end);
    } catch (CalendarNotConnectedException e) {
      // normal state, user just has not connected yet
      // prevented the system from reporting zero tracked hours if the user is not connected
      return ResponseEntity.ok(new CalendarVsTrackedResponse(false, 0, round2(trackedHours), 0));
    }

    // max - protection against negative Calendar durations

    /*

     Calendar event starts at 11pm on 10 Oct and it ends at 2am on 11 Oct
     The event was for 3 hhours
     but 1 hour is for 10th and rest to 11th
    */
    double calendarHours =
        events.stream()
            .filter(e -> e.getStartTime() != null && e.getEndTime() != null)
            .mapToDouble(
                e -> {
                  LocalDateTime eventStart =
                      e.getStartTime().isBefore(start) ? start : e.getStartTime();
                  LocalDateTime eventEnd = e.getEndTime().isAfter(end) ? end : e.getStartTime();
                  return Math.max(
                      0, Duration.between(eventStart, eventEnd).toSeconds() / SECONDS_PER_HOUR);
                })
            .sum();

    double unmatchedHours = Math.max(calendarHours - trackedHours, 0);

    return ResponseEntity.ok(
        new CalendarVsTrackedResponse(
            true, round2(calendarHours), round2(trackedHours), round2(unmatchedHours)));
  }
}
