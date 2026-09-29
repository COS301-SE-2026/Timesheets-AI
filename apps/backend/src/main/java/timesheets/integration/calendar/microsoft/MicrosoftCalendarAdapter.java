package timesheets.integration.calendar.microsoft;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import timesheets.auth.MicrosoftOAuthService;
import timesheets.auth.MicrosoftTokenResponse;
import timesheets.domain.IntegrationToken;
import timesheets.integration.calendar.CalendarAdapter;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.repository.IntegrationTokenRepository;

/*
- the adapter for the microsoft calendar
- it gets events from the microsoft graph
- then converts them into the shared calendar event

- it executes the microsot auth code and stores the resulting integration
*/

@Component
@RequiredArgsConstructor
public class MicrosoftCalendarAdapter implements CalendarAdapter {

  private final IntegrationTokenRepository integrationTokenRepository;
  private final MicrosoftOAuthService microsoftOAuthService;

  private static final String MICROSOFT_GRAPH_CALENDAR_URL =
      "https://graph.microsoft.com/v1.0/me/calendar/calendarView";

  private final RestClient restClient = RestClient.create();

  ZoneId southAfricaZone = ZoneId.of("Africa/Johannesburg");

  // the provider name
  @Override
  public String getProvider() {
    return "MICROSOFT_CALENDAR";
  }

  // gets the provided names between the specified dates
  @Override
  public List<CalendarEvent> getEvents(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    IntegrationToken token =
        integrationTokenRepository
            .findByWorkspaceMemberIdAndProvider(workspaceMemberId, "MICROSOFT_CALENDAR")
            .orElseThrow(() -> new RuntimeException("Microsoft Calendar is not connected"));

    String startDateTime = startTime.atZone(southAfricaZone).toOffsetDateTime().toString();
    String endDateTime = endTime.atZone(southAfricaZone).toOffsetDateTime().toString();

    String url =
        UriComponentsBuilder.fromHttpUrl(MICROSOFT_GRAPH_CALENDAR_URL)
            .queryParam("startDateTime", startDateTime)
            .queryParam("endDateTime", endDateTime)
            .build()
            .encode()
            .toUriString();

    MicrosoftGraphEventResponse response =
        restClient
            .get()
            .uri(url)
            .header("Authorization", "Bearer " + token.getAccessToken())
            .retrieve()
            .body(MicrosoftGraphEventResponse.class);

    if (response == null || response.getValue() == null) {
      return List.of();
    }

    return response.getValue().stream().map(this::mapToCalendarEvent).toList();
  }

  @Override
  public CalendarEvent getEvent(UUID workspaceMemberId, String externalEventId) {

    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public CalendarEvent createEvent(UUID workspaceMemberId, CalendarEvent event) {

    // right now the integration is read only
    return event;
  }

  @Override
  public CalendarEvent updateEvent(UUID workspaceMemberId, CalendarEvent event) {

    // right now the integration is read only
    return event;
  }

  @Override
  public void deleteEvent(UUID workspaceMemberId, String externalEventId) {
    // will implement later
  }

  // this is what will make the microsoft auth code into a token
  @Override
  public void exchangeAndsaveToken(UUID workspaceMemberId, String code) {

    MicrosoftTokenResponse tokenResponse = microsoftOAuthService.exchangeCodeForToken(code);
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn());

    // I'm storing the calendar token for more graph requests
    IntegrationToken integrationToken =
        IntegrationToken.builder()
            .workspaceMemberId(workspaceMemberId)
            .provider("MICROSOFT_CALENDAR")
            .accessToken(tokenResponse.getAccessToken())
            .refreshToken(tokenResponse.getRefreshToken())
            .expiresAt(expiresAt)
            .build();

    integrationTokenRepository.save(integrationToken);
  }

  // this converts the graph event into the calendar event that my app uses
  private CalendarEvent mapToCalendarEvent(MicrosoftGraphEvent microsoftEvent) {

    CalendarEvent calendarEvent = new CalendarEvent();

    calendarEvent.setTitle(microsoftEvent.getSubject());
    calendarEvent.setExternalEventId(microsoftEvent.getId());

    calendarEvent.setProvider(getProvider()); // telling frontend where the event is from

    if (microsoftEvent.getStart() != null && microsoftEvent.getStart().getDateTime() != null) {

      calendarEvent.setStartTime(LocalDateTime.parse(microsoftEvent.getStart().getDateTime()));
    }

    if (microsoftEvent.getEnd() != null && microsoftEvent.getEnd().getDateTime() != null) {

      calendarEvent.setEndTime(LocalDateTime.parse(microsoftEvent.getEnd().getDateTime()));
    }

    /*
    - this is where I take out the email addresses so that it uses the calendar event model
     */
    if (microsoftEvent.getAttendees() != null) {
      List<String> participants =
          microsoftEvent.getAttendees().stream()
              .filter(attendee -> attendee.getEmailAddress() != null)
              .map(attendee -> attendee.getEmailAddress().getAddress())
              .filter(address -> address != null && !address.isBlank())
              .toList();

      calendarEvent.setParticipants(participants);
    } else {
      calendarEvent.setParticipants(List.of());
    }

    return calendarEvent;
  }
}
