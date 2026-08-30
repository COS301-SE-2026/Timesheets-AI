package timesheets.integration.calendar.google;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;

public class GoogleCalendarAdapter implements CalendarAdapter {
  @Override
  public String getProvider() {
    // What is specified in dbs
    return "GOOGLE_CALENDAR";
  }

  @Override
  public List<CalendarEvent> getEvents(LocalDateTime startTime, LocalDateTime endTime) {
    return Collections.emptyList();
  }

  @Override
  public CalendarEvent createEvent(CalendarEvent event) {
    return event;
  }

  @Override
  public CalendarEvent updateEvent(CalendarEvent event) {
    return event;
  }

  @Override
  public void deleteEvent(String externalEventId) {}

  @Override
  public void exchangeAndsaveToken(UUID workspaceMemberId, String code) {}
}
