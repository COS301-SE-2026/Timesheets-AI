package timesheets.integration.calendar.microsoft;

import lombok.Data;

/*
- microsoft puts the attendees's email inside and emailAddress object
- this will match that structure for taking that data and making it a java object
 */
@Data
public class MicrosoftGraphAttendee {

  private MicrosoftGraphEmailAddress emailAddress;
}
