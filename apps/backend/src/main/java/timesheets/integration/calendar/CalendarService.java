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
    List<IntegrationToken> integrationTokens =
        integrationTokenRepository.findByWorkspaceMemberId(workspaceMemberId);

    return calendarAdapters.stream()
        .filter(
            adapter ->
                integrationTokens.stream()
                    .anyMatch(token -> token.getProvider().equals(adapter.getProvider())))
        .findFirst()
        .orElseThrow(CalendarNotConnectedException::new);
  }

  public CalendarStatus getStatus(UUID workspaceMemberId) {

    List<IntegrationToken> integrationTokens =
        integrationTokenRepository.findByWorkspaceMemberId(workspaceMemberId);

    return calendarAdapters.stream()
        .filter(
            adapter ->
                integrationTokens.stream()
                    .anyMatch(token -> token.getProvider().equals(adapter.getProvider())))
        .findFirst()
        .map(adapter -> new CalendarStatus(true, toDisplayProvider(adapter.getProvider()), null))
        .orElse(new CalendarStatus(false, null, null));
  }

  private String toDisplayProvider(String provider) {
    return switch (provider) {
      case "GOOGLE_CALENDAR" -> "google";
      case "MICROSOFT_CALENDAR" -> "microsoft";
      default -> provider.toLowerCase();
    };
  }
}
