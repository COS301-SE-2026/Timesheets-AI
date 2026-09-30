package timesheets.integration.calendar.microsoft;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/*
- returns the response by the microsoft graph
- the values will be in a value array
 */
@Data
public class MicrosoftGraphEventResponse {

  private List<MicrosoftGraphEvent> value;

  // this will be the link when another page of results is available
  @JsonProperty("@odata.nextLink")
  private String nextLink;
}
