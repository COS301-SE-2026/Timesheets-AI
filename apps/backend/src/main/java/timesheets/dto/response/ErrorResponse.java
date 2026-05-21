package timesheets.dto.response;

import java.util.UUID;

public class ErrorResponse {
    
    private Integer status;
    private String error;
    private String message;
    private UUID activeTimerId;
    
    public ErrorResponse() {}
    

    //? multiple constructors because I could have errors that have various parameters, I could expand this quite a bit actually

    //this could be for any HTTP error
    public ErrorResponse(Integer status, String error, String message, UUID activeTimerId) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.activeTimerId = activeTimerId;
    }


    //this could be to indicate a timer conflict if the user tries to create a second timer
    public ErrorResponse(String message, UUID activeTimerId) {
        this.message = message;
        this.activeTimerId = activeTimerId;
        this.status = 409;
        this.error = "Conflict";
    }


    //I could use this one for simple errors without extra data
    public ErrorResponse(String message) {
        this.message = message;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public UUID getActiveTimerId() {
        return activeTimerId;
    }
    
    public void setActiveTimerId(UUID activeTimerId) {
        this.activeTimerId = activeTimerId;
    }
}