package timesheets.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.AccountDeletionRequest;
import timesheets.dto.response.MessageResponse;
import timesheets.service.AccountService;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @Operation(
      summary = "Request account deletion",
      description = "Submits an account deletion request for the authenticated user.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Account deletion request submitted successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid account deletion request"),
    @ApiResponse(responseCode = "401", description = "User is not authenticated"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "409", description = "Account deletion request cannot be processed")
  })
  @PostMapping("/deletion/request")
  public ResponseEntity<MessageResponse> requestDeletion(
      @Valid @RequestBody AccountDeletionRequest.Request request) {

    accountService.requestDeletion(request);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(
            new MessageResponse(
                "Account deletion request submitted successfully. An admin will review your request."));
  }
}
