package timesheets.integration.calendar.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import timesheets.auth.GoogleOAuthService;
import timesheets.domain.IntegrationToken;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.repository.IntegrationTokenRepository;

@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements CalendarAdapter {

  @Value("${app.google.client-id}")
  private String googleClientId;

  @Value("${app.google.client-secret}")
  private String googleClientSecret;

  private final IntegrationTokenRepository integrationTokenRepository;
  private final GoogleOAuthService googleOAuthService;

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
    // GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken);

    /* after testing, google credentials (line above) just give the access token so when it expires, it cannot refresh it (got error related here)
       UserCredentials (object created here) has accessToken, refreshToken and expiresAt

       will create userCredentials
    */

   UserCredentials credentials = UserCredentials.newBuilder().setClientId(googleClientId).setClientSecret(googleClientSecret).setAccessToken(googleAccessToken).setRefresh(refreshToken).build();
    HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

    // To get the user's calendar events
    // this communicate with the google api
    Calendar calendarService =
        new Calendar.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("Momently")
            .build();

    // putting all the events in here
    // making the request to get collection of calendar events and it is stored Events event
    // insert try and catch because request() might fail

    Events events;

    try {
      events =
          calendarService
              .events()
              .list("primary")
              .setTimeMin(
                  new com.google.api.client.util.DateTime(
                      startTime.toInstant(ZoneOffset.UTC).toEpochMilli()))
              .setTimeMax(
                  new com.google.api.client.util.DateTime(
                      endTime.toInstant(ZoneOffset.UTC).toEpochMilli()))
              .setSingleEvents(true)
              .setOrderBy("startTime")
              .execute();
    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve Google Calendar events", e);
    }

    // for returning, convert to match CalendarEvent expected vars
    List<Event> googleEvents = events.getItems();

    // conversion
    List<CalendarEvent> calendarEvents = new ArrayList<>();

    for (Event googleEvent : googleEvents) {
      CalendarEvent calendarEvent = new CalendarEvent();

      calendarEvent.setTitle(googleEvent.getSummary());
      calendarEvent.setExternalEventId(googleEvent.getId());

      // The times is stored differently in Google
      // google stores as googlevent - getStart() and getEnd(): EventDateTime objects
      // I am using LocalDateTime

      if (googleEvent.getStart().getDateTime() != null
          && googleEvent.getEnd().getDateTime() != null) {
        LocalDateTime eventStartTime =
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
                ZoneId.systemDefault());

        LocalDateTime eventEndTime =
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue()),
                ZoneId.systemDefault());

        calendarEvent.setStartTime(eventStartTime);
        calendarEvent.setEndTime(eventEndTime);
      } else if (googleEvent.getStart().getDate() != null
          && googleEvent.getEnd().getDate() != null) {

        LocalDate eventStartDate = LocalDate.parse(googleEvent.getStart().getDate().toString());
        LocalDate eventEndDate = LocalDate.parse(googleEvent.getEnd().getDate().toString());

        calendarEvent.setStartTime(eventStartDate.atStartOfDay());
        calendarEvent.setEndTime(eventEndDate.atStartOfDay());
      }

      calendarEvents.add(calendarEvent);
    }

    return calendarEvents;
  }

  @Override
  public CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId) {
    Optional<IntegrationToken> integrationToken =
        integrationTokenRepository.findByWorkspaceMemberIdAndProvider(
            workspaceMemberId, "GOOGLE_CALENDAR");

    if (integrationToken.isEmpty()) {
      throw new RuntimeException("Google Calendar is not connected");
    }

    IntegrationToken token = integrationToken.get();
    String accessToken = token.getAccessToken();

    Date expirationTime =
        Date.from(token.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant());

    AccessToken googleAccessToken = new AccessToken(accessToken, expirationTime);

    GoogleCredentials credentials = GoogleCredentials.create(googleAccessToken);

    HttpCredentialsAdapter requestInitializer = new HttpCredentialsAdapter(credentials);

    Calendar calendarService =
        new Calendar.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
            .setApplicationName("Momently")
            .build();

    Event googleEvent;

    try {
      googleEvent = calendarService.events().get("primary", externalEventId).execute();
    } catch (IOException e) {
      throw new RuntimeException("Failed to retrieve Google Calendar event", e);
    }

    CalendarEvent calendarEvent = new CalendarEvent();

    calendarEvent.setTitle(googleEvent.getSummary());
    calendarEvent.setExternalEventId(googleEvent.getId());

    if (googleEvent.getStart().getDateTime() != null
        && googleEvent.getEnd().getDateTime() != null) {

      LocalDateTime eventStartTime =
          LocalDateTime.ofInstant(
              java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
              ZoneId.systemDefault());

      LocalDateTime eventEndTime =
          LocalDateTime.ofInstant(
              java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue()),
              ZoneId.systemDefault());

      calendarEvent.setStartTime(eventStartTime);
      calendarEvent.setEndTime(eventEndTime);
    } else if (googleEvent.getStart().getDate() != null && googleEvent.getEnd().getDate() != null) {
      LocalDate eventStartDate = LocalDate.parse(googleEvent.getStart().getDate().toString());

      LocalDate eventEndDate = LocalDate.parse(googleEvent.getEnd().getDate().toString());

      calendarEvent.setStartTime(eventStartDate.atStartOfDay());
      calendarEvent.setEndTime(eventEndDate.atStartOfDay());
    }

    return calendarEvent;
  }
}
