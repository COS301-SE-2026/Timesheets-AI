package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.SendProjectMessageRequest;
import timesheets.dto.request.ToggleReactionRequest;
import timesheets.dto.response.ProjectMessageLocationResponse;
import timesheets.dto.response.ProjectMessageResponse;
import timesheets.service.ProjectCommunityService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProjectCommunityController {

  private final ProjectCommunityService communityService;

  // the project's conversation, oldest first
  @GetMapping("/projects/{projectId}/messages")
  public ResponseEntity<List<ProjectMessageResponse>> getMessages(@PathVariable UUID projectId) {
    return ResponseEntity.ok(communityService.getMessages(projectId));
  }

  @PostMapping("/projects/{projectId}/messages")
  public ResponseEntity<ProjectMessageResponse> sendMessage(
      @PathVariable UUID projectId, @Valid @RequestBody SendProjectMessageRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(communityService.sendMessage(projectId, request.getContent()));
  }

  @DeleteMapping("/projects/{projectId}/messages/{messageId}")
  public ResponseEntity<Void> deleteMessage(
      @PathVariable UUID projectId, @PathVariable UUID messageId) {
    communityService.deleteMessage(projectId, messageId);
    return ResponseEntity.noContent().build();
  }

  // adds or removes my reaction, returns the updated message
  @PostMapping("/projects/{projectId}/messages/{messageId}/reactions")
  public ResponseEntity<ProjectMessageResponse> toggleReaction(
      @PathVariable UUID projectId,
      @PathVariable UUID messageId,
      @Valid @RequestBody ToggleReactionRequest request) {
    return ResponseEntity.ok(
        communityService.toggleReaction(projectId, messageId, request.getReactionType()));
  }

  // used when a notification is clicked
  @GetMapping("/project-messages/{messageId}/location")
  public ResponseEntity<ProjectMessageLocationResponse> getMessageLocation(
      @PathVariable UUID messageId) {
    return ResponseEntity.ok(communityService.getMessageLocation(messageId));
  }
}
