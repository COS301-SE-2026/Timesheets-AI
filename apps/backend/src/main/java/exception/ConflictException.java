package exception;

import java.util.UUID;


//this is an exception that the user will get when they try to run a timer, that is already running
//I am doing this such that the frontend can handle things properly, remember how proper error codes help frontend and users
public class ConflictException extends RuntimeException {
    
    private final String userMessage;
    private final UUID activeTimerId;
    
    public ConflictException(String message, String userMessage, UUID activeTimerId) {

        super(message); //remember inheritance so the parent class RuntimeException class is the one that is called, 

        this.userMessage = userMessage;
        this.activeTimerId = activeTimerId;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
    
    public UUID getActiveTimerId() {
        return activeTimerId;
    }
}