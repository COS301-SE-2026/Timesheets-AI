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
import timesheets.dto.request.AssignWorkspaceMemberRequest;
import timesheets.dto.response.AvailableUserResponse;
import timesheets.dto.response.WorkspaceMemberResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.TeamService;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;
  private final SecurityUtils securityUtils;

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

  /*
  ADMIN: this will be to get all the users
  - then the users can filter based on the flag whether they want to list all the users, list the users in the workspace, or list those that are not in the workspace
  - frontend will be able to use this and then filter from there


  */
  @GetMapping("/members/available")
  public ResponseEntity<List<AvailableUserResponse>> getAvailableUsers() {

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    List<AvailableUserResponse> users = teamService.getAvailableUsers(workspaceId);

    return ResponseEntity.ok(users);
  }
}
