package timesheets.integration.calendar.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import timesheets.auth.GoogleOAuthService;
import timesheets.auth.GoogleTokenResponse;
import timesheets.domain.IntegrationToken;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.integration.calendar.CalendarNotConnectedException;
import timesheets.repository.IntegrationTokenRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements CalendarAdapter {

  private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");
  private static final String PROVIDER = "GOOGLE_CALENDAR";

  @Value("${app.google.client-id}")
  private String googleClientId;

  @Value("${app.google.client-secret}")
  private String googleClientSecret;

  private final IntegrationTokenRepository integrationTokenRepository;
  private final GoogleOAuthService googleOAuthService;

  @Override
  public String getProvider() {
    return PROVIDER;
  }

  @Override
  public List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {
    if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
      throw new IllegalArgumentException("startTime must be before endTime");
    }

    IntegrationToken token = findToken(workspaceMemberId);
    UserCredentials credentials = buildCredentials(token);

    try {
      credentials.refreshIfExpired();
      persistRefreshedToken(token, credentials);

      Calendar calendar = buildCalendar(credentials);
      Events response =
          calendar
              .events()
              .list("primary")
              .setTimeMin(
                  new com.google.api.client.util.DateTime(
                      startTime.atZone(SAST).toInstant().toEpochMilli()))
              .setTimeMax(
                  new com.google.api.client.util.DateTime(
                      endTime.atZone(SAST).toInstant().toEpochMilli()))
              .setSingleEvents(true)
              .setOrderBy("startTime")
              .execute();

      List<CalendarEvent> result = new ArrayList<>();
      if (response.getItems() != null) {
        for (Event event : response.getItems()) {
          result.add(toCalendarEvent(event));
        }
      }
      return result;
    } catch (IOException e) {
      log.error(
          "Google Calendar events request failed for workspace member {}: {}",
          workspaceMemberId,
          e.getMessage(),
          e);
      throw new RuntimeException("Failed to retrieve Google Calendar events", e);
    } finally {
      // The Google client can refresh automatically while executing a
      persistRefreshedToken(token, credentials);
    }
  }

  @Override
  public CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId) {

    IntegrationToken token = findToken(workspaceMemberId);
    UserCredentials credentials = buildCredentials(token);

    try {
      credentials.refreshIfExpired();
      persistRefreshedToken(token, credentials);
      Event event = buildCalendar(credentials).events().get("primary", externalEventId).execute();
      return toCalendarEvent(event);
    } catch (IOException e) {
      log.error(
          "Google Calendar event request failed for workspace member {}: {}",
          workspaceMemberId,
          e.getMessage(),
          e);
      throw new RuntimeException("Failed to retrieve Google Calendar event", e);
    } finally {
      persistRefreshedToken(token, credentials);
    }
  }

  private IntegrationToken findToken(UUID workspaceMemberId) {
    return integrationTokenRepository
        .findByWorkspaceMemberIdAndProvider(workspaceMemberId, PROVIDER)
        .orElseThrow(CalendarNotConnectedException::new);
  }

  private UserCredentials buildCredentials(IntegrationToken token) {
    Date expiration =
        token.getExpiresAt() == null
            ? null
            : Date.from(token.getExpiresAt().atZone(SAST).toInstant());

    return UserCredentials.newBuilder()
        .setClientId(googleClientId)
        .setClientSecret(googleClientSecret)
        .setAccessToken(new AccessToken(token.getAccessToken(), expiration))
        .setRefreshToken(token.getRefreshToken())
        .build();
  }

  private Calendar buildCalendar(UserCredentials credentials) {
    return new Calendar.Builder(
            new NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials))
        .setApplicationName("Momently")
        .build();
  }

  private void persistRefreshedToken(IntegrationToken token, UserCredentials credentials) {
    AccessToken current = credentials.getAccessToken();
    if (current == null || current.getTokenValue() == null) {
      return;
    }

    Date expiration = current.getExpirationTime();
    LocalDateTime expiresAt =
        expiration == null ? null : LocalDateTime.ofInstant(expiration.toInstant(), SAST);

    if (!Objects.equals(token.getAccessToken(), current.getTokenValue())
        || !Objects.equals(token.getExpiresAt(), expiresAt)) {
      token.setAccessToken(current.getTokenValue());
      token.setExpiresAt(expiresAt);
      integrationTokenRepository.save(token);
      log.info(
          "Persisted refreshed Google Calendar access token for member {}",
          token.getWorkspaceMemberId());
    }
  }

  private CalendarEvent toCalendarEvent(Event event) {
    CalendarEvent result = new CalendarEvent();
    result.setTitle(event.getSummary());
    result.setExternalEventId(event.getId());
    result.setProvider(PROVIDER);

    List<String> participants = new ArrayList<>();
    if (event.getAttendees() != null) {
      for (EventAttendee attendee : event.getAttendees()) {
        if (attendee.getEmail() != null) {
          participants.add(attendee.getEmail());
        }
      }
    }
    result.setParticipants(participants);

    if (event.getStart() == null || event.getEnd() == null) {
      return result;
    }

    if (event.getStart().getDateTime() != null && event.getEnd().getDateTime() != null) {
      result.setStartTime(
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(event.getStart().getDateTime().getValue()), SAST));
      result.setEndTime(
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(event.getEnd().getDateTime().getValue()), SAST));
    } else if (event.getStart().getDate() != null && event.getEnd().getDate() != null) {
      LocalDate startDate = LocalDate.parse(event.getStart().getDate().toString());
      LocalDate endDate = LocalDate.parse(event.getEnd().getDate().toString());
      result.setStartTime(startDate.atStartOfDay());
      result.setEndTime(endDate.atStartOfDay());
    }
    return result;
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
  public void exchangeAndsaveToken(UUID workspaceMemberId, String code) {
    GoogleTokenResponse response = googleOAuthService.exchangeCodeforToken(code);
    LocalDateTime expiresAt = LocalDateTime.now(SAST).plusSeconds(response.getExpiresIn());

    IntegrationToken token =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, PROVIDER)
            .orElseGet(IntegrationToken::new);
    token.setWorkspaceMemberId(workspaceMemberId);
    token.setProvider(PROVIDER);
    token.setAccessToken(response.getAccessToken());
    // Google may omit a refresh token on subsequent authorizations.
    if (response.getRefreshToken() != null && !response.getRefreshToken().isBlank()) {
      token.setRefreshToken(response.getRefreshToken());
    }
    token.setExpiresAt(expiresAt);
    integrationTokenRepository.save(token);
  }
}
