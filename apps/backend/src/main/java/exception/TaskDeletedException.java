package exception;

import java.util.UUID;

// thrown when one tries to access a deleted task
public class TaskDeletedException extends RuntimeException {

  public TaskDeletedException(UUID id) {
    super("Task has been deleted with id: " + id);
  }

  public TaskDeletedException(String message) {
    super(message);
  }
}
