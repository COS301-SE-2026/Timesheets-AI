package timesheets.config;

import exception.ConflictException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import exception.TimeEntryAccessDeniedException;
import exception.TimeEntryNotFoundException;
import exception.UnauthorizedException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import timesheets.dto.response.ErrorResponse;
import timesheets.dto.response.MessageResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 400 - Bad Request
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new MessageResponse(message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(400, "Bad Request", e.getMessage(), null));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(400, "Bad Request", e.getMessage(), null));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(400, "Bad Request", e.getMessage(), null));
  }

  // 403 Forbidden
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "Forbidden", e.getMessage(), null));
  }

  @ExceptionHandler(TimeEntryAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleTimeEntryAccessDenied(
      TimeEntryAccessDeniedException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "Forbidden", e.getMessage(), null));
  }

  // 404 Not Found
  @ExceptionHandler(TimeEntryNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTimeEntryNotFound(TimeEntryNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, "Not Found", e.getMessage(), null));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, "Not Found", e.getMessage(), null));
  }

  // 409 Conflict
  @ExceptionHandler(StateConflictException.class)
  public ResponseEntity<ErrorResponse> handleStateConflict(StateConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "Conflict", e.getMessage(), null));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.conflict(e.getUserMessage(), e.getActiveTimerId()));
  }

  // 500 Internal Server
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
    // Log the exception here if you have logging
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred", null));
  }
}
