package timesheets.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.dto.response.ProjectResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final SecurityUtils securityUtils;
  private final ProjectService projectService;

  @GetMapping
  public ResponseEntity<List<ProjectResponse>> getProjects() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    List<ProjectResponse> projects =
        projectService.getProjectsForUser(workspaceMemberId, isAdmin, isManager);

    return ResponseEntity.ok(projects);
  }
}
