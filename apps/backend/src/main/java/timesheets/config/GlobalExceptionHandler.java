package timesheets.config;

import exception.AuthException;
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

  /*
  ! 400 - Bad Request
  - this will be for when there are validation errors
  - for example: when there are missing fields or when there are constraint violations
  */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException ex) {

    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return ResponseEntity.badRequest().body(new MessageResponse(message));
  }

  // this will handle the authentication error code
  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {

    // this will map the auth code to a valid HTTP code
    HttpStatus status = mapErrorCodeToHttpStatus(e.getErrorCode());

    return ResponseEntity.status(status)
        .body(new ErrorResponse(status.value(), status.getReasonPhrase(), e.getMessage()));
  }

  /*
  ! 403 - Forbidden
  - this will be when there are authorization errors
  - if the user is user is authenticated but they do not have access to that thing
  - for example: dev trying to approve a timesheet or a dev trying to create a project
  */
  // this will be for handling unauthorised access attempts
  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "Forbidden", e.getMessage()));
  }

  // handling access attempts on time entries
  @ExceptionHandler(TimeEntryAccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleTimeEntryAccessDenied(
      TimeEntryAccessDeniedException e) {

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse(403, "Forbidden", e.getMessage()));
  }

  // ! 404 - Not Found, for cases where the resources are not found
  // handles when time entries are not found
  @ExceptionHandler(TimeEntryNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleTimeEntryNotFound(TimeEntryNotFoundException e) {

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, "Not Found", e.getMessage()));
  }

  // generic when resources are not found
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(404, "Not Found", e.getMessage()));
  }

  // ! 409 - Conflict, operation not performed because of the state of resource
  // this will be for the state conflicts
  @ExceptionHandler(StateConflictException.class)
  public ResponseEntity<ErrorResponse> handleStateConflict(StateConflictException e) {

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(409, "Conflict", e.getMessage()));
  }

  // to handle when there is conflict with the timer
  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.conflict(e.getUserMessage(), e.getActiveTimerId()));
  }

  // ! 500 - Internal Server Error
  // for now I am doing this for all unhandled exceptions, will replace it with more specific ones
  // moving forward
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"));
  }

  // ! mapping the AuthException error codes to the HTTP codes

  private HttpStatus mapErrorCodeToHttpStatus(AuthException.ErrorCode errorCode) {
    switch (errorCode) {
      case INVALID_CREDENTIALS:
        return HttpStatus.UNAUTHORIZED;
      case ACCOUNT_LOCKED:
        return HttpStatus.LOCKED; // this will be 423
      case SSO_USER:
      case ACCOUNT_NOT_CONFIGURED:
        return HttpStatus.FORBIDDEN;
      case EMAIL_EXISTS:
        return HttpStatus.CONFLICT;
      case EMAIL_DOMAIN:
      case TOKEN_NOT_FOUND:
      case TOKEN_EXPIRED:
      case TOKEN_USED:
      case USER_NOT_FOUND:
      default:
        return HttpStatus.BAD_REQUEST;
    }
  }
}
