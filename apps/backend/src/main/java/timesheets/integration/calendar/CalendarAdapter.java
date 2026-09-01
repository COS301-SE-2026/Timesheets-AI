package timesheets.integration.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import timesheets.integration.IntegrationAdapter;

public interface CalendarAdapter extends IntegrationAdapter {
  // read
  List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime);

  // write
  // external provider will generate an ID when event is created
  // Google creates the event and gives us an ID
  // Tha adapter can return title, startTime, endTime and externalEventId

  // read one event 
  CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId);
  
  CalendarEvent createEvent(UUID workspaceMemberId, CalendarEvent event);

  // when we update it, the adapter needs to know which external event
  CalendarEvent updateEvent(UUID workspaceMemberId, CalendarEvent event);

  // delte using eternaleventId
  void deleteEvent(UUID workspaceMemberId, String externalEventId);
}
