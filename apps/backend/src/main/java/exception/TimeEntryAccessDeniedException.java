package exception;

// this is an exception that the user gets when they try to access a time entry that they do not have access to
public class TimeEntryAccessDeniedException extends RuntimeException{
    public TimeEntryAccessDeniedException(String message) {
        super(message);
    }
}
