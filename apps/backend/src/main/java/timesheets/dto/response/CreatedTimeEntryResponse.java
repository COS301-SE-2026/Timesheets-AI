package timesheets.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class CreatedTimeEntryResponse {

  private UUID id;
  private SimpleProject project;
  private SimpleTask task;
  private LocalDate date;

  private LocalTime startTime;
  private LocalTime endTime;

  private Integer durationMinutes;
  private String status;

  public CreatedTimeEntryResponse() {}

  public CreatedTimeEntryResponse(
      UUID id,
      SimpleProject project,
      SimpleTask task,
      LocalDate date,
      LocalTime startTime,
      LocalTime endTime,
      Integer durationMinutes,
      String status) {
    this.id = id;
    this.project = project;
    this.task = task;
    this.date = date;

    this.startTime = startTime;
    this.endTime = endTime;

    this.durationMinutes = durationMinutes;
    this.status = status;
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

  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

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
