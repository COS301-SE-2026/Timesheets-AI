package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;

@Component
@RequiredArgsConstructor
public class CalendarEvidenceCollector implements EvidenceCollector {
  private final CalendarAdapter calendarAdapter;

  @Override
  public List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    // Collect events
    List<CalendarEvent> calendarEvents =
        calendarAdapter.getEvents(workspaceMemberId, startTime, endTime);

    List<EvidenceEvent> evidenceEvents = new ArrayList<>();

    for (CalendarEvent calendarEvent : calendarEvents) {
      EvidenceEvent evidenceEvent = new EvidenceEvent();

      evidenceEvent.setId(UUID.randomUUID());
      evidenceEvent.setSource("CALENDAR");
      evidenceEvent.setTimestamp(calendarEvent.getStartTime());
      evidenceEvent.setWorkspaceMemberId(workspaceMemberId);
      evidenceEvent.setActivityType("MEETING");
      evidenceEvent.setDescription(calendarEvent.getTitle());

      Map<String, Object> metadata = new HashMap<>();
      metadata.put("externalEventId", calendarEvent.getExternalEventId());
      metadata.put("endTime", calendarEvent.getEndTime());

      evidenceEvent.setMetadata(metadata);
      evidenceEvents.add(evidenceEvent);
    }

    return evidenceEvents;
  }
}
