package timesheets.integration.calendar.google;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.domain.IntegrationToken;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.repository.IntegrationTokenRepository;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;

@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements CalendarAdapter {

  private final IntegrationTokenRepository integrationTokenRepository;

  @Override
  public String getProvider() {
    // What is specified in dbs
    return "GOOGLE_CALENDAR";
  }

  @Override
  public List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    Optional<IntegrationToken> integrationToken =
        integrationTokenRepository.findByWorkspaceMemberIdAndProvider(
            workspaceMemberId, "GOOGLE_CALENDAR");

    // token is not found
    if (integrationToken.isEmpty()) {
      throw new RuntimeException("Google Calendar is not connected");
    }

    // token exists

    IntegrationToken token = integrationToken.get();

    String accessToken = token.getAccessToken();

    return Collections.emptyList();
  }

  @Override
  public CalendarEvent createEvent(UUID workspaceMemberId, CalendarEvent event) {
    return event;
  }

  @Override
  public CalendarEvent updateEvent(UUID workspaceMemberId, CalendarEvent event) {
    return event;
  }

  @Override
  public void deleteEvent(UUID workspaceMemberId, String externalEventId) {}

  @Override
  public void exchangeAndsaveToken(UUID workspaceMemberId, String code) {}
}
