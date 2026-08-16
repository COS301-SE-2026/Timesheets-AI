package exception;

// this will be used when one tries to change a time entry that is locked
public class TimeEntryLockedException extends StateConflictException {

  public TimeEntryLockedException(String message) {
    super(message);
  }
}
