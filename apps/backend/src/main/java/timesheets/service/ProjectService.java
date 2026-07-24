package timesheets.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import timesheets.dto.response.ProjectDetailResponse;
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

  // creates a new project in the current workspace
  @Transactional
  public ProjectResponse createProject(
      CreateProjectRequest request, UUID createdByWorkspaceMemberId) {
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
            .orElseThrow(() -> new RuntimeException("Workspace member not found: " + managerId));

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

  // gets detailed information about a project
  @Transactional
  public ProjectDetailResponse getProjectDetail(UUID projectId, UUID workspaceMemberId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

    // to verify if the user has access to the project
    if (!userHasAccessToProject(projectId, workspaceMemberId)) {
      throw new RuntimeException("No access to this project");
    }

    // finds all the members assigned to this project
    List<ProjectMember> projectMembers = projectMemberRepository.findByProjectId(projectId);

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

  // ! helper functions
  // determines a users role on a project
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
                      .orElseThrow(() -> new RuntimeException("Member not found"));

              // gets the user details
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(() -> new RuntimeException("User not found"));

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
            .updatedAt(project.getUpdatedAt());

    if (showCostInfo) {
      builder
          .budgetHours(project.getBudgetHours())
          .hourlyRate(project.getHourlyRate())
          .budgetCost(project.getBudgetCost());

      if (project.getBudgetHours() != null && project.getHourlyRate() != null) {
        BigDecimal totalCost = project.getBudgetHours().multiply(project.getHourlyRate());
        builder.totalCost(totalCost);
      }
    }
    return builder.build();
  }
}
