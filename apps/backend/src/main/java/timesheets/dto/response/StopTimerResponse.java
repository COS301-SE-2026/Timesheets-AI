package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class StopTimerResponse {
    
    private UUID timerId;
    private LocalDateTime stoppedAt;
    private Integer durationMinutes;
    private CreatedTimeEntryResponse createdTimeEntry;
    
    public StopTimerResponse() {}
    
    public StopTimerResponse(UUID timerId, LocalDateTime stoppedAt, Integer durationMinutes, CreatedTimeEntryResponse createdTimeEntry) {
        this.timerId = timerId;
        this.stoppedAt = stoppedAt;
        this.durationMinutes = durationMinutes;
        this.createdTimeEntry = createdTimeEntry;
    }
    
    public UUID getTimerId() {
        return timerId;
    }
    
    public void setTimerId(UUID timerId) {
        this.timerId = timerId;
    }
    
    public LocalDateTime getStoppedAt() {
        return stoppedAt;
    }
    
    public void setStoppedAt(LocalDateTime stoppedAt) {
        this.stoppedAt = stoppedAt;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public CreatedTimeEntryResponse getCreatedTimeEntry() {
        return createdTimeEntry;
    }
    
    public void setCreatedTimeEntry(CreatedTimeEntryResponse createdTimeEntry) {
        this.createdTimeEntry = createdTimeEntry;
    }
}