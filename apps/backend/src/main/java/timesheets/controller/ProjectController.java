package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.request.AssignProjectMemberRequest;
import timesheets.dto.request.CreateProjectRequest;
import timesheets.dto.request.UpdateProjectRequest;
import timesheets.dto.response.ProjectDetailResponse;
import timesheets.dto.response.ProjectMemberResponse;
import timesheets.dto.response.ProjectResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final SecurityUtils securityUtils;
  private final ProjectService projectService;

  // this will get the projects - role based access implemented in my service file
  @GetMapping
  public ResponseEntity<List<ProjectResponse>> getProjects() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    List<ProjectResponse> projects =
        projectService.getProjectsForUser(workspaceMemberId, isAdmin, isManager);

    return ResponseEntity.ok(projects);
  }

  // this will get detailed info about a particular project
  @GetMapping("/{projectId}")
  public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable UUID projectId) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // verifies the user can view this project
    if (!projectService.userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new RuntimeException("You don't have access to this project");
    }

    ProjectDetailResponse response = projectService.getProjectDetail(projectId, workspaceMemberId);

    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<ProjectResponse> createProject(
      @Valid @RequestBody CreateProjectRequest request) {

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    if (!isAdmin && !isManager) {
      throw new RuntimeException("Only Admins and Managers can create projects");
    }

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    ProjectResponse response = projectService.createProject(request, workspaceMemberId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // this will be used to update a project
  @PatchMapping("/{projectId}")
  public ResponseEntity<ProjectResponse> patchProject(
      @PathVariable UUID projectId, @RequestBody UpdateProjectRequest request) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    ProjectResponse response = projectService.updateProject(projectId, request, workspaceMemberId);
    return ResponseEntity.ok(response);
  }

  // this should archive a project
  @PatchMapping("/{projectId}/archive")
  public ResponseEntity<Void> archiveProject(@PathVariable UUID projectId) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    projectService.archiveProject(projectId, workspaceMemberId);

    return ResponseEntity.ok().build();
  }

  // to delete a project
  @DeleteMapping("/{projectId}")
  public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {

    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    projectService.deleteProject(projectId, workspaceMemberId);

    return ResponseEntity.noContent().build();
  }

  // this will be to assign a workspace member to a project
  @PostMapping("/{projectId}/members")
  public ResponseEntity<ProjectMemberResponse> assignMemberToProject(
      @PathVariable UUID projectId, @Valid @RequestBody AssignProjectMemberRequest request) {

    ProjectMemberResponse response =
        projectService.assignMemberToProject(
            projectId, request.getWorkspaceMemberId(), request.getIsProjectManager());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  // this will delete a member from the project
  @DeleteMapping("/{projectId}/members/{workspaceMemberId}")
  public ResponseEntity<Void> removeMemberFromProject(
      @PathVariable UUID projectId, @PathVariable UUID workspaceMemberId) {

    projectService.removeMemberFromProject(projectId, workspaceMemberId);

    return ResponseEntity.noContent().build();
  }
}
