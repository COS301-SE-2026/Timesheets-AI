package timesheets.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.AssignWorkspaceMemberRequest;
import timesheets.dto.response.WorkspaceMemberResponse;
import timesheets.service.TeamService;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  // ADMIN: this will assign a user to the current workspace
  @PostMapping("/members")
  public ResponseEntity<WorkspaceMemberResponse> assignUserToWorkspace(
      @Valid @RequestBody AssignWorkspaceMemberRequest request) {

    WorkspaceMemberResponse response = teamService.assignUserToWorkspace(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // ADMIN: this should delete a user from the workspace
  @DeleteMapping("/members/{workspaceMemberId}")
  public ResponseEntity<Void> removeUserFromWorkspace(@PathVariable UUID workspaceMemberId) {

    teamService.removeUserFromWorkspace(workspaceMemberId);

    return ResponseEntity.noContent().build();
  }
}
