package timesheets.service;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.domain.TimeEntry;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.CreateProjectRequest;
import timesheets.dto.request.UpdateProjectRequest;
import timesheets.dto.response.ProjectDetailResponse;
import timesheets.dto.response.ProjectMemberResponse;
import timesheets.dto.response.ProjectResponse;
import timesheets.enums.WorkspaceRole;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final SecurityUtils securityUtils;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final TimeEntryRepository timeEntryRepository;
  private final UserRepository userRepository;

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
          // projectMemberRepository.findByWorkspaceMemberId(workspaceMemberId).stream()
          projectMemberRepository.findByWorkspaceMemberIdAndIsActiveTrue(workspaceMemberId).stream()
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

  // creates a new project in the current workspace
  @Transactional
  public ProjectResponse createProject(
      CreateProjectRequest request, UUID createdByWorkspaceMemberId) {

    if (!securityUtils.isAdmin()) {
      throw new AccessDeniedException("Only Admins can create projects");
    }

    UUID workspaceId = securityUtils.getCurrentWorkspaceId();

    // to build the project from the request
    Project project = new Project();
    project.setWorkspaceId(workspaceId);
    project.setName(request.getName());
    project.setDescription(request.getDescription());
    project.setBudgetHours(request.getBudgetHours());
    project.setHourlyRate(request.getHourlyRate());
    project.setStatus("ACTIVE");
    project.setIsDeleted(false);
    project.setCreatedByWorkspaceMemberId(createdByWorkspaceMemberId);
    project.setStartDate(request.getStartDate());
    project.setEndDate(request.getEndDate());

    // this will calculate the budget cost
    BigDecimal budgetCost = request.getBudgetCost();
    if (budgetCost == null && request.getBudgetHours() != null && request.getHourlyRate() != null) {
      budgetCost = request.getBudgetHours().multiply(request.getHourlyRate());
    }
    project.setBudgetCost(budgetCost);

    Project savedProject = projectRepository.save(project); // saving project to the DB

    // to assign project managers if they are assigned - mostly for admin
    if (request.getManagerIds() != null && !request.getManagerIds().isEmpty()) {
      for (UUID managerId : request.getManagerIds()) {
        workspaceMemberRepository
            .findById(managerId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Workspace member not found: " + managerId));

        ProjectMember member = new ProjectMember();
        member.setProjectId(savedProject.getId());
        member.setWorkspaceMemberId(managerId);
        member.setIsProjectManager(true);
        member.setIsActive(true);
        projectMemberRepository.save(member);
      }
    }

    // made the show cost info true since only an admin or manager can create a project anyway
    return buildProjectResponse(savedProject, createdByWorkspaceMemberId, true);
  }

  /*
  - ADMIN: Can update any project
  - MANAGER: Can update any project in their workspace
  - PROJECT MANAGER: Can update projects they manage
  - DEVELOPER: Cannot update projects
  */
  @Transactional
  public ProjectResponse updateProject(
      UUID projectId, UpdateProjectRequest request, UUID workspaceMemberId) {

    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Project not found with id: " + projectId));

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();
    boolean isProjectManager = isProjectManager(projectId, workspaceMemberId);

    if (!isAdmin && !isManager && !isProjectManager) {
      throw new AccessDeniedException(
          "Only Admins, Managers, and Project Managers can update projects");
    }

    // if a project is archived then it cannot be updated
    if ("ARCHIVED".equals(project.getStatus())) {
      throw new StateConflictException("Cannot update an archived project");
    }

    // the provided details will be the one updated
    if (request.getName() != null) {
      project.setName(request.getName());
    }

    if (request.getDescription() != null) {
      project.setDescription(request.getDescription());
    }

    if (request.getBudgetHours() != null) {
      project.setBudgetHours(request.getBudgetHours());
    }

    if (request.getHourlyRate() != null) {
      project.setHourlyRate(request.getHourlyRate());
    }

    if (request.getStartDate() != null) {
      project.setStartDate(request.getStartDate());
    }

    if (request.getEndDate() != null) {
      project.setEndDate(request.getEndDate());
    }

    if (request.getBudgetCost() != null) {
      project.setBudgetCost(request.getBudgetCost());
    }

    Project updatedProject = projectRepository.save(project);

    return buildProjectResponse(updatedProject, workspaceMemberId, true);
  }

  // this is for archiving a project
  @Transactional
  public void archiveProject(UUID projectId, UUID workspaceMemberId) {

    // only admins should be able to archive projects
    if (!securityUtils.isAdmin()) {
      throw new AccessDeniedException("Only Admins can archive projects");
    }

    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Project not found with id: " + projectId));

    // we cannot archive already archived projects
    if ("ARCHIVED".equals(project.getStatus())) {
      throw new StateConflictException("Project is already archived");
    }

    project.setStatus("ARCHIVED");
    project.setUpdatedAt(LocalDateTime.now());
    projectRepository.save(project);
  }

  // this is for deleting a project
  @Transactional
  public void deleteProject(UUID projectId, UUID workspaceMemberId) {

    if (!securityUtils.isAdmin()) {
      throw new AccessDeniedException("Only Admins can delete projects");
    }

    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Project not found with id: " + projectId));

    // projects that are already deleted cannot be deleted
    if (Boolean.TRUE.equals(project.getIsDeleted())) {
      throw new StateConflictException("Project is already deleted");
    }

    // keep in mind that we only do soft deletes
    project.setIsDeleted(true);
    project.setDeletedAt(LocalDateTime.now());
    projectRepository.save(project);
  }

  // gets detailed information about a project
  @Transactional
  public ProjectDetailResponse getProjectDetail(UUID projectId, UUID workspaceMemberId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Project not found with id: " + projectId));

    // to verify if the user has access to the project
    if (!userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new AccessDeniedException("No access to this project");
    }

    // finds all the members assigned to this project
    List<ProjectMember> projectMembers =
        projectMemberRepository.findByProjectIdAndIsActiveTrue(projectId);

    // should build all the info on the members in that project
    List<ProjectDetailResponse.MemberInfo> memberInfos =
        buildMemberInfos(projectId, projectMembers);

    BigDecimal totalHoursLogged = calculateProjectTotalHours(projectId);
    BigDecimal progressPercentage = calculateProgressPercentage(project, totalHoursLogged);

    boolean showCostInfo = securityUtils.isAdmin() || securityUtils.isManager();

    // calling the helper to build the response
    return buildProjectDetailResponse(
        project, memberInfos, totalHoursLogged, progressPercentage, showCostInfo);
  }

  @Transactional(readOnly = true)
  public boolean userHasAccessToProject(UUID projectId, UUID workspaceMemberId) {
    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    if (isAdmin || isManager) {
      return true;
    }

    // only devs that are assigned to this project, otherwise false and has no access
    return projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
        projectId, workspaceMemberId);
  }

  /*
  ADMIN: and admin can assign anyone to a project
  MANAGER: manager can only assign those in their workspaces
  PROJECT_MANAGERS: they can assign people to the projects
  DEV: cannot assign anyone
  */
  @Transactional
  public ProjectMemberResponse assignMemberToProject(
      UUID projectId, UUID workspaceMemberId, Boolean isProjectManager) {

    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

    WorkspaceMember member =
        workspaceMemberRepository
            .findById(workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Workspace member not found"));

    // to ensure that the user belongs to the same workspace as the project
    if (!member.getWorkspaceId().equals(project.getWorkspaceId())) {
      throw new AccessDeniedException("Member does not belong to the project's workspace");
    }

    // these are the only people who have access to assigning a member to a project
    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();

    /*
    - theoratically they can and should be able to
    - but the UI right now prevents them because they cannot even see the members in the workspace
    - the reason I say this is because someone can be a developer at a workspace level but a project manager
    - so the UI prevents them from seeing the teams tab
    - also look into: public List<AvailableUserResponse> getAvailableUsers(UUID workspaceId) in TeamService to see if you can make project mangers view all users
    */
    boolean isProjectManagerRole = isProjectManager(projectId, currentMemberId);

    if (!isAdmin && !isManager && !isProjectManagerRole) {
      throw new AccessDeniedException(
          "Only Admins and Managers and Project Managers can assign members to projects");
    }

    // if a member is already assigned to a project then we cannot do that again
    if (projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
        projectId, workspaceMemberId)) {
      throw new StateConflictException("Member is already assigned to this project");
    }

    // creating that member
    ProjectMember projectMember = new ProjectMember();
    projectMember.setProjectId(projectId);
    projectMember.setWorkspaceMemberId(workspaceMemberId);
    projectMember.setIsProjectManager(isProjectManager != null && isProjectManager);
    projectMember.setIsActive(true);
    projectMember.setCreatedAt(LocalDateTime.now());
    projectMember.setUpdatedAt(LocalDateTime.now());

    ProjectMember saved = projectMemberRepository.save(projectMember);

    User user =
        userRepository
            .findById(member.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    return ProjectMemberResponse.builder()
        .projectMemberId(saved.getId())
        .workspaceMemberId(saved.getWorkspaceMemberId())
        .userId(user.getId())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .isProjectManager(saved.getIsProjectManager())
        .joinedAt(saved.getCreatedAt())
        .build();
  }

  // this is about deleting members from a project
  @Transactional
  public void removeMemberFromProject(UUID projectId, UUID workspaceMemberId) {
    UUID currentMemberId = securityUtils.getDefaultWorkspaceMemberId();

    // need to see if the project actually exists
    if (!projectRepository.existsById(projectId)) {
      throw new ResourceNotFoundException("Project not found");
    }

    boolean isAdmin = securityUtils.isAdmin();
    boolean isManager = securityUtils.isManager();
    boolean isProjectManager = isProjectManager(projectId, currentMemberId);

    if (!isAdmin && !isManager && !isProjectManager) {
      throw new AccessDeniedException(
          "Only Admins and Managers and Project Managers can remove members from projects");
    }

    ProjectMember projectMember =
        projectMemberRepository
            .findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found on this project"));

    // this is soft deleting so that we keep track
    projectMember.setIsActive(false);
    projectMember.setUpdatedAt(LocalDateTime.now());
    projectMemberRepository.save(projectMember);
  }

  // ! helper functions
  // determines a users role on a project
  private WorkspaceRole getProjectLevelRole(UUID projectId, UUID workspaceMemberId) {
    return projectMemberRepository
        .findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
        .map(
            projectMembership ->
                projectMembership.getIsProjectManager()
                    ? WorkspaceRole.MANAGER
                    : WorkspaceRole.DEVELOPER)
        .orElse(null);
  }

  // to determine if a user has management permissions for a specific project
  private boolean isProjectManager(UUID projectId, UUID workspaceMemberId) {
    return projectMemberRepository
        .findByProjectIdAndWorkspaceMemberId(projectId, workspaceMemberId)
        .map(pm -> Boolean.TRUE.equals(pm.getIsProjectManager()))
        .orElse(false);
  }

  // calculates total project hours
  private BigDecimal calculateProjectTotalHours(UUID projectId) {
    List<TimeEntry> entries = timeEntryRepository.findByProjectId(projectId);

    /*
    - gets the duration in seconds
    - want to remove nulls
    -convert to seconds
    - sum it all up
     */
    return entries.stream()
        .map(TimeEntry::getDurationSeconds)
        .filter(duration -> duration != null)
        .map(duration -> BigDecimal.valueOf(duration / 60.0))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // builds a list with detailed info about each project member
  private List<ProjectDetailResponse.MemberInfo> buildMemberInfos(
      UUID projectId, List<ProjectMember> projectMembers) {
    return projectMembers.stream()
        .map(
            projectMembership -> {

              // gets the workspace member
              WorkspaceMember member =
                  workspaceMemberRepository
                      .findById(projectMembership.getWorkspaceMemberId())
                      .orElseThrow(
                          () ->
                              new ResourceNotFoundException(
                                  "Workspace member not found with id: "
                                      + projectMembership.getWorkspaceMemberId()));

              // gets the user details
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(
                          () ->
                              new ResourceNotFoundException(
                                  "User not found with id: " + member.getUserId()));

              // calculate the hours logged for the project
              BigDecimal hoursLogged =
                  calculateMemberHours(projectId, projectMembership.getWorkspaceMemberId());

              return ProjectDetailResponse.MemberInfo.builder()
                  .workspaceMemberId(projectMembership.getWorkspaceMemberId())
                  .firstName(user.getFirstName())
                  .lastName(user.getLastName())
                  .email(user.getEmail())
                  .role(
                      projectMembership.getIsProjectManager()
                          ? WorkspaceRole.MANAGER
                          : WorkspaceRole.DEVELOPER)
                  .hoursLogged(hoursLogged)
                  .joinedAt(projectMembership.getCreatedAt())
                  .build();
            })
        .collect(Collectors.toList());
  }

  // caluclates the hours by a member for a certain projet
  private BigDecimal calculateMemberHours(UUID projectId, UUID workspaceMemberId) {
    List<TimeEntry> entries =
        timeEntryRepository.findByWorkspaceMemberIdAndProjectId(workspaceMemberId, projectId);

    /*
    - gets the seconds from each entry
    - removes the nulls
    - concerts the seconds to minutes
    - sums all the minutes (from 0) */
    return entries.stream()
        .map(TimeEntry::getDurationSeconds)
        .filter(duration -> duration != null)
        .map(duration -> BigDecimal.valueOf(duration / 60.0))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /*
  - calculates the progress using hours and budget hours
  - formula: (hoursLogged / budgetHours)*100
  */
  private BigDecimal calculateProgressPercentage(Project project, BigDecimal totalHoursLogged) {

    // if there is no budgetHours then it is hard to see the progress
    if (project.getBudgetHours() == null
        || project.getBudgetHours().compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    /*
    - divides logged hours by budget hours to 4 dec places
    - multiples by 100
    - rounds to 2 dec places */
    BigDecimal completePercentage =
        totalHoursLogged
            .divide(project.getBudgetHours(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    return completePercentage;
  }

  // ! builder helper function
  private ProjectResponse buildProjectResponse(
      Project project, UUID workspaceMemberId, boolean showCostInfo) {
    WorkspaceRole role = getProjectLevelRole(project.getId(), workspaceMemberId);

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
            .updatedAt(project.getUpdatedAt())
            .budgetHours(project.getBudgetHours())
            .hourlyRate(project.getHourlyRate());

    if (showCostInfo) {
      builder.budgetCost(project.getBudgetCost());
    }

    return builder.build();
  }

  private ProjectDetailResponse buildProjectDetailResponse(
      Project project,
      List<ProjectDetailResponse.MemberInfo> memberInfos,
      BigDecimal totalHoursLogged,
      BigDecimal progressPercentage,
      boolean showCostInfo) {

    ProjectDetailResponse.ProjectDetailResponseBuilder builder =
        ProjectDetailResponse.builder()
            .id(project.getId())
            .name(project.getName())
            .description(project.getDescription())
            .status(project.getStatus())
            .members(memberInfos)
            .hoursLogged(totalHoursLogged)
            .progressPercentage(progressPercentage)
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .budgetHours(project.getBudgetHours())
            .hourlyRate(project.getHourlyRate());

    if (showCostInfo) {
      builder.budgetCost(project.getBudgetCost());

      if (project.getBudgetHours() != null && project.getHourlyRate() != null) {
        BigDecimal totalCost = project.getBudgetHours().multiply(project.getHourlyRate());
        builder.totalCost(totalCost);
      }
    }
    return builder.build();
  }
}
