package timesheets.integration.calendar;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.domain.IntegrationToken;
import timesheets.repository.IntegrationTokenRepository;

@Service
@RequiredArgsConstructor
public class CalendarService {
  private final List<CalendarAdapter> calendarAdapters;
  private final IntegrationTokenRepository integrationTokenRepository;

  public List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    CalendarAdapter calendarAdapter = getCalendarAdapter(workspaceMemberId);
    return calendarAdapter.getEvents(workspaceMemberId, startTime, endTime);
  }

  public CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId) {
    CalendarAdapter calendarAdapter = getCalendarAdapter(workspaceMemberId);
    return calendarAdapter.getEvent(workspaceMemberId, externalEventId);
  }

  private CalendarAdapter getCalendarAdapter(UUID workspaceMemberId) {

    // to find which calendar provider this user has connected
    IntegrationToken integrationToken =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "GOOGLE_CALENDAR")
            .orElse(null);

    if (integrationToken == null) {
      throw new CalendarNotConnectedException();
    }

    String provider = integrationToken.getProvider();

    return calendarAdapters.stream()
        .filter(adapter -> adapter.getProvider().equals(provider))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No calendar adapter found for provider: " + provider));
  }
}
