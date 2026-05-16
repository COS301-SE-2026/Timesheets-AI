package exception;

//this will be thrown when the time entry is not found with a specific id

public class TimeEntryNotFoundException extends RuntimeException {
    public TimeEntryNotFoundException(Long id) {
        super("Time entry not found with id: " + id);
    }
}
