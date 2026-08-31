package exception;

/*
- this is an exception that is thrown when there is a conflict with the current state

for example:
- submitting an already submitted timesheet
- editing a locked time entry
- approving a non-submitted timesheet

this should be an HTTP 409 Conflict
*/
public class StateConflictException extends RuntimeException {
  public StateConflictException(String message) {
    super(message);
  }
}
