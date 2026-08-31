package timesheets.integration.calendar.google;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import com.google.api.services.calendar.model.Event;
import java.util.ArrayList;

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

    for (Event googleEvent : googleEvents){
      CalendarEvent calendarEvent = new CalendarEvent();
      calendarEvent.setTitle(googleEvent.getSummary());
      calendarEvent.setExternalEventId(googleEvent.getId());
      calendarEvents.add(calendarEvent);
    }

    return calendarEvents;
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
