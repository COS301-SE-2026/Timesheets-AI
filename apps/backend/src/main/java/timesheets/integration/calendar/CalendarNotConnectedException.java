package timesheets.integration.calendar;

public class CalendarNotConnectedException extends RuntimeException {
  public CalendarNotConnectedException() {
    super("google calendar is not connected");
  }
}
