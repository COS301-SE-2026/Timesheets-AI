package timesheets.integration.calendar.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import timesheets.domain.IntegrationToken;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.repository.IntegrationTokenRepository;

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

    // this token provide Momently to gain persmission to the user's Google calendar
    // google libraries wraps tokens in objects rather raw strings, ojects allows us to track
    // expiration time

    String accessToken = token.getAccessToken();

    Date expirationTime =
        Date.from(token.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant());

    AccessToken googleAccessToken = new AccessToken(accessToken, expirationTime);

    // GoogleCredentials is Google authentication object, it will wrap the AccessToken in a format
    // that the Google's API libraries understand
    GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken);

    HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

    // To get the user's calendar events
    Calendar calendarService =
        new Calendar.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("Momently")
            .build();

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
