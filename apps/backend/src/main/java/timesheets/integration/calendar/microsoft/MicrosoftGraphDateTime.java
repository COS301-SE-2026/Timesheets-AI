package timesheets.integration.calendar.microsoft;

import lombok.Data;

/*
- microsoft give back the date and time and its timezone as a nested object
- this is to help Java make it a java object
 */
@Data
public class MicrosoftGraphDateTime {

  private String dateTime;
  private String timeZone;
}
