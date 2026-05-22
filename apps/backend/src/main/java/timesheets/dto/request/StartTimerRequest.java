package timesheets.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

//remember this is like essentially what backend is expecting from frontend (from the JSON) then it create an object here (by deserializing)
//what I mean by desearilizing - converting data from a stored format into a usable object

public class StartTimerRequest {
    
    @NotNull(message = "Project ID is required") //this means that when the response comes the projectID field must not be NULL
    private UUID projectId;

    private UUID taskId;
    private String notes;
    
    public UUID getProjectId() {
        return projectId;
    }
    
    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }
    
    public UUID getTaskId() {
        return taskId;
    }
    
    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}