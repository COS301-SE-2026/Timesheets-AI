package timesheets.integration.calendar.microsoft;

import java.util.List;
import lombok.Data;

/*
- this will be the event that comes from microsoft graph
 */
@Data
public class MicrosoftGraphEvent {

  // this will be the identifier from microsoft graph
  private String id;

  // microsoft returns the title as subject
  private String subject;

  private MicrosoftGraphDateTime start;
  private MicrosoftGraphDateTime end;
  private List<MicrosoftGraphAttendee> attendees;
}
