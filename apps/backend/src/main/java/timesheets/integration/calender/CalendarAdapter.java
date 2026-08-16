package timesheets.integration.calendar;

import java.time.LocalDateTime;
import java.util.List;
import timesheets.integration.IntegrationAdapter;

public interface CalendarAdapter extends IntegrationAdapter {
  // read
  List<CalenderEvent> getEvents(LocalDateTime startTime, LocalDateTime endTime);

  // write
  // external provider will generate an ID when event is created
  // Google creates the event and gives us an ID
  // Tha adapter can return title, startTime, endTime and externalEventId

  CalenderEvent createEvent(CalenderEvent event);

  // when we update it, the adapter needs to know which external event 
  CalenderEvent updateEvent(CalenderEvent event);

    // delte using eternaleventId
  void deleteEvent(String externalEventId);
}
