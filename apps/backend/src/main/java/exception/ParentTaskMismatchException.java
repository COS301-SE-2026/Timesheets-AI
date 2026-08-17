package exception;

// this will be thrown when the parent task is not in the same project
public class ParentTaskMismatchException extends RuntimeException {

  public ParentTaskMismatchException(String message) {
    super(message);
  }
}
