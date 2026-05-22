package exception;

import java.util.UUID;

//this will be thrown when the time entry is not found with a specific id

public class TimeEntryNotFoundException extends RuntimeException {
    public TimeEntryNotFoundException(UUID id) {
        super("Time entry not found with id: " + id);
    }
}
