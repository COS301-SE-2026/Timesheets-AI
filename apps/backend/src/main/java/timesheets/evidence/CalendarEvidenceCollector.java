package timesheets.evidence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.integration.calendar.CalendarService;

@Component
@RequiredArgsConstructor
public class CalendarEvidenceCollector implements EvidenceCollector {
  private final CalendarService calendarService;

  @Override
  public List<EvidenceEvent> collect(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    // Collect events
    List<CalendarEvent> calendarEvents =
        calendarService.getEvents(workspaceMemberId, startTime, endTime);

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
      metadata.put("title", calendarEvent.getTitle());
      metadata.put("startTime", calendarEvent.getStartTime());
      metadata.put("endTime", calendarEvent.getEndTime());
      metadata.put("participants", calendarEvent.getParticipants());

      evidenceEvent.setMetadata(metadata);
      evidenceEvents.add(evidenceEvent);
    }

    return evidenceEvents;
  }
}
