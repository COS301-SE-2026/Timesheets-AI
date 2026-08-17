package exception;

import java.util.UUID;

// this will be found when a task is not found
public class TaskNotFoundException extends RuntimeException {

  public TaskNotFoundException(UUID id) {
    super("Task not found with id: " + id);
  }

  public TaskNotFoundException(String message) {
    super(message);
  }
}
