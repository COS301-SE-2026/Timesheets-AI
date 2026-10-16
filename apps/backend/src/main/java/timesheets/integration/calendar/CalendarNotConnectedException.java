package timesheets.integration.calendar;

public class CalendarNotConnectedException extends RuntimeException {
  public CalendarNotConnectedException() {
    super("No calendar provider is connected");
  }
}
