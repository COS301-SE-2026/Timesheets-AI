package timesheets.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.dto.response.ProjectResponse;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final SecurityUtils securityUtils;

  /*
  - gets all the projects for the current user
  - admin should see all the projects across workspaces
  - manager should see all their workspace
  - developer will see only the projects they are assigned to */
  @Transactional
  public List<ProjectResponse> getProjectsForUser(
      UUID workspaceMemberId, boolean isAdmin, boolean isManager) {

    List<Project> projects;

    if (isAdmin) {
      projects = projectRepository.findAllByIsDeletedFalse();
    } else if (isManager) {
      UUID workspaceId = securityUtils.getCurrentWorkspaceId();
      projects = projectRepository.findByWorkspaceIdAndIsDeletedFalse(workspaceId);
    } else {
      List<UUID> projectIds =
          projectMemberRepository.findByWorkspaceMemberId(workspaceMemberId).stream()
              .map(ProjectMember::getProjectId)
              .collect(Collectors.toList());

      if (projectIds.isEmpty()) {
        return List.of();
      }

      projects = projectRepository.findAllById(projectIds);
    }

    boolean showCostInfo = isAdmin || isManager;

    return projects.stream()
        .map(project -> buildProjectResponse(project, workspaceMemberId, showCostInfo))
        .collect(Collectors.toList());
  }

  // ! helper functions
  private WorkspaceRole getProjectRole(UUID projectId, UUID workspaceMemberId) {
    return projectMemberRepository
        .findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
        .map(
            projectMembership ->
                projectMembership.getIsProjectManager()
                    ? WorkspaceRole.MANAGER
                    : WorkspaceRole.DEVELOPER)
        .orElse(null);
  }

  // ! builder helper function
  private ProjectResponse buildProjectResponse(
      Project project, UUID workspaceMemberId, boolean showCostInfo) {
    WorkspaceRole role = getProjectRole(project.getId(), workspaceMemberId);

    ProjectResponse.ProjectResponseBuilder builder =
        ProjectResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .status(project.getStatus())
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .myRole(role)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt());

    if (showCostInfo) {
      builder
          .budgetHours(project.getBudgetHours())
          .hourlyRate(project.getHourlyRate())
          .budgetCost(project.getBudgetCost());
    }

    return builder.build();
  }
}
