package timesheets.controller;

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

  // user requests for account deletion
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
