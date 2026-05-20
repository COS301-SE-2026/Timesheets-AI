package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;


//exactly the structure that the frontend is expecting
public class ActiveTimerResponse {
    
    private UUID id;
    private SimpleProject project;
    private SimpleTask task;

    private LocalDateTime startedAt;
    private Integer elapsedMinutes;

    private Boolean active;
    
    public ActiveTimerResponse() {}
    
    public ActiveTimerResponse(UUID id, SimpleProject project, SimpleTask task, LocalDateTime startedAt, Integer elapsedMinutes, Boolean active) {
        this.id = id;
        this.project = project;
        this.task = task;

        this.startedAt = startedAt;
        this.elapsedMinutes = elapsedMinutes;
        
        this.active = active;
    }
    
    public static ActiveTimerResponse empty() {

        ActiveTimerResponse response = new ActiveTimerResponse();
        response.setActive(false); //this will say that the timer is inactive

        return response;
    }
    

    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public SimpleProject getProject() {
        return project;
    }
    
    public void setProject(SimpleProject project) {
        this.project = project;
    }
    
    public SimpleTask getTask() {
        return task;
    }
    
    public void setTask(SimpleTask task) {
        this.task = task;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public Integer getElapsedMinutes() {
        return elapsedMinutes;
    }
    
    public void setElapsedMinutes(Integer elapsedMinutes) {
        this.elapsedMinutes = elapsedMinutes;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    


    //! using inner classes 

    //this is so that I can quickly have all the info about a task and a project in one call about the Timer, such that, I do not have to call the API multiple times yeah??
    public static class SimpleProject {
        private UUID id;
        private String name;
        
        public SimpleProject() {}
        
        public SimpleProject(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public UUID getId() {
            return id;
        }
        
        public void setId(UUID id) {
            this.id = id;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }

    //! another inner class
    public static class SimpleTask {
        private UUID id;
        private String title;
        
        public SimpleTask() {}
        
        public SimpleTask(UUID id, String title) {
            this.id = id;
            this.title = title;
        }
        
        public UUID getId() {
            return id;
        }
        
        public void setId(UUID id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
    }
}