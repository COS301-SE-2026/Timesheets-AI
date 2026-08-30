package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import exception.AccessDeniedException;
import exception.ResourceNotFoundException;
import exception.StateConflictException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProjectService Unit Tests")
public class ProjectServiceTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private WorkspaceMemberRepository workspaceMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private TimeEntryRepository timeEntryRepository;

  @InjectMocks private ProjectService projectService;

  private final UUID testUserId = UUID.randomUUID();
  private final UUID testProjectId = UUID.randomUUID();
  private final UUID testWorkspaceId = UUID.randomUUID();
  private final String testProjectName = "Test Project";
  private final String testProjectDescription = "Test Description";
  private final BigDecimal testBudgetHours = BigDecimal.valueOf(100);
  private final BigDecimal testHourlyRate = BigDecimal.valueOf(50);
  private final BigDecimal testBudgetCost = BigDecimal.valueOf(5000);
  private final UUID testWorkspaceMemberId = UUID.randomUUID();

  // ! helper functions
  /*
  - for this one, I am not using BeforeEach and a setup because each test requires diff setups
  - like I would be mocking unnecessarily if I do that
  */
  private Project createTestProject() {
    Project project = new Project();
    project.setId(testProjectId);
    project.setWorkspaceId(testWorkspaceId);
    project.setName(testProjectName);
    project.setDescription(testProjectDescription);
    project.setBudgetHours(testBudgetHours);
    project.setHourlyRate(testHourlyRate);
    project.setBudgetCost(testBudgetCost);
    project.setStatus("ACTIVE");
    project.setIsDeleted(false);
    project.setCreatedByWorkspaceMemberId(testWorkspaceMemberId);
    project.setStartDate(LocalDate.now());
    project.setEndDate(LocalDate.now().plusMonths(1));
    project.setCreatedAt(LocalDateTime.now());
    project.setUpdatedAt(LocalDateTime.now());
    return project;
  }

  private ProjectMember createTestProjectMember() {
    ProjectMember member = new ProjectMember();
    member.setProjectId(testProjectId);
    member.setWorkspaceMemberId(testWorkspaceMemberId);
    member.setIsProjectManager(false);
    member.setIsActive(true);
    member.setCreatedAt(LocalDateTime.now());
    return member;
  }

  private Project createArchivedProject() {
    Project project = createTestProject();
    project.setStatus("ARCHIVED");
    return project;
  }

  private Project createDeletedProject() {
    Project project = createTestProject();
    project.setIsDeleted(true);
    project.setDeletedAt(LocalDateTime.now());
    return project;
  }

  private CreateProjectRequest createValidCreateProjectRequest() {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setName(testProjectName);
    request.setDescription(testProjectDescription);
    request.setBudgetHours(testBudgetHours);
    request.setHourlyRate(testHourlyRate);
    request.setStartDate(LocalDate.now());
    request.setEndDate(LocalDate.now().plusMonths(1));
    request.setManagerIds(List.of());
    return request;
  }

  private UpdateProjectRequest createValidUpdateProjectRequest() {
    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated Project Name");
    request.setDescription("Updated Description");
    request.setBudgetHours(BigDecimal.valueOf(200));
    request.setHourlyRate(BigDecimal.valueOf(75));
    request.setStartDate(LocalDate.now().plusDays(1));
    request.setEndDate(LocalDate.now().plusMonths(2));
    request.setBudgetCost(BigDecimal.valueOf(15000));
    return request;
  }

  private User createTestUser() {
    User user = new User();
    user.setId(testUserId);
    user.setEmail("test@momentum.co.za");
    user.setFirstName("Test");
    user.setLastName("User");
    return user;
  }

  private WorkspaceMember createTestWorkspaceMember() {
    WorkspaceMember member = new WorkspaceMember();
    member.setId(testWorkspaceMemberId);
    member.setUserId(testUserId);
    member.setWorkspaceId(testWorkspaceId);
    member.setRole(WorkspaceRole.DEVELOPER);
    member.setCreatedAt(LocalDateTime.now());
    return member;
  }

  private TimeEntry createTestTimeEntry() {
    TimeEntry entry = new TimeEntry();
    entry.setId(UUID.randomUUID());
    entry.setProjectId(testProjectId);
    entry.setWorkspaceMemberId(testWorkspaceMemberId);
    entry.setDurationSeconds(3600);
    return entry;
  }

  @Nested
  @DisplayName("Get Projects Tests")
  class GetProjectsTests {

    @Test
    @DisplayName("return all projects for admin")
    void returnAllProjectsAdmin() {

      // ARRANGE: setting up the admin and the project
      Project project = createTestProject();
      List<Project> projects = List.of(project);

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findAllByIsDeletedFalse()).thenReturn(projects);

      // ACT: the projects are taken
      List<ProjectResponse> responses =
          projectService.getProjectsForUser(testWorkspaceMemberId, true, false);

      // ASSERT: remember an admin should be able to see all the cost info
      assertThat(responses).isNotNull();
      assertThat(responses).hasSize(1); // only one project
      assertThat(responses.get(0).getName()).isEqualTo(testProjectName);
      assertThat(responses.get(0).getBudgetHours()).isEqualTo(testBudgetHours);
      assertThat(responses.get(0).getHourlyRate()).isEqualTo(testHourlyRate);
      assertThat(responses.get(0).getBudgetCost()).isEqualTo(testBudgetCost);

      verify(projectRepository)
          .findAllByIsDeletedFalse(); // making sure that the correct repo is called
    }

    @Test
    @DisplayName("return workspace projects for manager")
    void returnWorkspaceProjectsForManager() {
      Project project = createTestProject();
      List<Project> projects = List.of(project);

      when(securityUtils.isManager()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(projectRepository.findByWorkspaceIdAndIsDeletedFalse(testWorkspaceId))
          .thenReturn(projects);

      List<ProjectResponse> responses =
          projectService.getProjectsForUser(testWorkspaceMemberId, false, true);

      assertThat(responses).isNotNull();
      assertThat(responses).hasSize(1);
      assertThat(responses.get(0).getName()).isEqualTo(testProjectName);
      assertThat(responses.get(0).getBudgetHours()).isEqualTo(testBudgetHours);
      assertThat(responses.get(0).getHourlyRate()).isEqualTo(testHourlyRate);
      assertThat(responses.get(0).getBudgetCost()).isEqualTo(testBudgetCost);

      verify(projectRepository).findByWorkspaceIdAndIsDeletedFalse(testWorkspaceId);
    }

    @Test
    @DisplayName("return assigned projects for developer")
    void returnAssignedProjectsForDeveloper() {
      Project project = createTestProject();
      List<Project> projects = List.of(project);
      ProjectMember projectMember = createTestProjectMember();

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);
      when(projectMemberRepository.findByWorkspaceMemberId(testWorkspaceMemberId))
          .thenReturn(List.of(projectMember));
      when(projectRepository.findAllById(List.of(testProjectId))).thenReturn(projects);

      List<ProjectResponse> responses =
          projectService.getProjectsForUser(testWorkspaceMemberId, false, false);

      assertThat(responses).isNotNull();
      assertThat(responses).hasSize(1);
      assertThat(responses.get(0).getName()).isEqualTo(testProjectName);

      // developer should not see the cost info
      assertThat(responses.get(0).getBudgetCost()).isNull();

      verify(projectRepository).findAllById(List.of(testProjectId));
    }

    @Test
    @DisplayName("return empty list when developer has no assigned projects")
    void returnEmptyListWhenDeveloperHasNoProjects() {
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);
      when(projectMemberRepository.findByWorkspaceMemberId(testWorkspaceMemberId))
          .thenReturn(List.of());

      List<ProjectResponse> responses =
          projectService.getProjectsForUser(testWorkspaceMemberId, false, false);

      assertThat(responses).isNotNull();
      assertThat(responses).isEmpty();

      verify(projectRepository, never()).findAllById(any());
    }
  }

  @Nested
  @DisplayName("Create Projects Tests")
  class CreateProjectsTests {

    @Test
    @DisplayName("create project successfully")
    void createProject() {

      // ARRANGE: setup project creation request
      CreateProjectRequest request = createValidCreateProjectRequest();

      // simulates what the project returns from DB
      Project savedProject = createTestProject();

      // specifying that the user is an admin
      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.isManager()).thenReturn(false);

      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(projectRepository.save(any(Project.class))).thenReturn(savedProject);

      // determines the users role on a project
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember()));

      /*
      - ACT: Create project
      - the service: sets the workspace ID
      - creates a new project entity
      - calculates the budget cost
      - saves the project to DB
      - assignes project managers
      */
      ProjectResponse response = projectService.createProject(request, testWorkspaceMemberId);

      // ASSERT: Verify project was created
      assertThat(response).isNotNull();
      assertThat(response.getName()).isEqualTo(testProjectName);
      assertThat(response.getBudgetHours()).isEqualTo(testBudgetHours);
      assertThat(response.getHourlyRate()).isEqualTo(testHourlyRate);
      assertThat(response.getBudgetCost()).isEqualTo(testBudgetCost);

      verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when non-admin tries to create project")
    void throwExceptionWhenNonAdminCreatesProject() {
      CreateProjectRequest request = createValidCreateProjectRequest();

      when(securityUtils.isAdmin()).thenReturn(false);

      assertThatThrownBy(() -> projectService.createProject(request, testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins can create projects");

      verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("assign project managers when provided")
    void assignProjectManagersWhenProvided() {

      CreateProjectRequest request = createValidCreateProjectRequest();
      request.setManagerIds(List.of(testWorkspaceMemberId));

      Project savedProject = createTestProject();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestWorkspaceMember()));

      when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember()));

      ProjectResponse response = projectService.createProject(request, testWorkspaceMemberId);

      assertThat(response).isNotNull();
      verify(projectMemberRepository, times(1)).save(any(ProjectMember.class));
    }
  }

  @Nested
  @DisplayName("Delete Project Tests")
  class DeleteProjectTests {
    @Test
    @DisplayName("delete project successfully")
    void deleteProjectSuccessfully() {
      Project project = createTestProject();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(projectRepository.save(any(Project.class))).thenReturn(project);

      projectService.deleteProject(testProjectId, testWorkspaceMemberId);

      assertThat(project.getIsDeleted()).isTrue();
      assertThat(project.getDeletedAt()).isNotNull();
      verify(projectRepository).save(project);
    }

    @Test
    @DisplayName("throw exception when non-admin tries to delete project")
    void throwExceptionWhenNonAdminDeletesProject() {

      when(securityUtils.isAdmin()).thenReturn(false);

      assertThatThrownBy(() -> projectService.deleteProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins can delete projects");

      verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when deleting already deleted project")
    void throwExceptionWhenDeletingDeletedProject() {

      Project deletedProject = createDeletedProject();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(deletedProject));

      assertThatThrownBy(() -> projectService.deleteProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Project is already deleted");

      verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when project not found for deletion")
    void throwExceptionWhenProjectNotFoundForDelete() {

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> projectService.deleteProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Project not found with id: " + testProjectId);
    }
  }

  @Nested
  @DisplayName("Get Project Detail Tests")
  class GetProjectDetailTests {

    @Test
    @DisplayName("returns project details")
    void returnProjectDetail() {

      // ARRANGE: setting up the project and the members
      Project project = createTestProject();
      WorkspaceMember workspaceMember = createTestWorkspaceMember();
      ProjectMember projectMember = createTestProjectMember();

      User user = createTestUser();
      TimeEntry timeEntry = createTestTimeEntry();

      // I want for the project to be returned when searched by ID
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectMemberRepository.findByProjectId(testProjectId))
          .thenReturn(List.of(projectMember));
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));
      when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

      // this should be used to calculate the hours logged on the project
      when(timeEntryRepository.findByProjectId(testProjectId)).thenReturn(List.of(timeEntry));

      when(timeEntryRepository.findByWorkspaceMemberIdAndProjectId(
              testWorkspaceMemberId, testProjectId))
          .thenReturn(List.of(timeEntry));
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(projectMember));

      // ACT: get the project details
      ProjectDetailResponse response =
          projectService.getProjectDetail(testProjectId, testWorkspaceMemberId);

      // ASSERT: checking all the project details that are returned
      assertThat(response).isNotNull();
      assertThat(response.getId()).isEqualTo(testProjectId);
      assertThat(response.getName()).isEqualTo(testProjectName);
      assertThat(response.getMembers()).isNotNull();
      assertThat(response.getMembers()).hasSize(1);
      assertThat(response.getHoursLogged()).isEqualTo(BigDecimal.valueOf(60.0));

      assertThat(response.getProgressPercentage()).isEqualByComparingTo(BigDecimal.valueOf(60.00));

      verify(projectRepository).findById(testProjectId);
    }

    @Test
    @DisplayName("throw exception when user has no access to project")
    void throwExceptionWhenUserHasNoAccessToProject() {

      Project project = createTestProject();

      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);
      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(false);

      assertThatThrownBy(
              () -> projectService.getProjectDetail(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("No access to this project");
    }

    @Test
    @DisplayName("throw exception when project not found")
    void throwExceptionWhenProjectNotFoundForDetail() {

      when(projectRepository.findById(testProjectId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> projectService.getProjectDetail(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Project not found with id: " + testProjectId);
    }
  }

  @Nested
  @DisplayName("create projects tests")
  class CreateProjectTests {

    @Test
    @DisplayName("budget cost calculation")
    void calculateBudgetCost() {

      // ARRANGE: request without a budget
      CreateProjectRequest request = createValidCreateProjectRequest();
      request.setBudgetCost(null);

      Project savedProject = createTestProject();

      // specifying that the user is an admin
      when(securityUtils.isAdmin()).thenReturn(true);
      when(securityUtils.isManager()).thenReturn(false);

      when(securityUtils.getCurrentWorkspaceId()).thenReturn(testWorkspaceId);
      when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember()));

      // ACT: creating the project
      ProjectResponse response = projectService.createProject(request, testWorkspaceMemberId);

      // ASSERT: want to see that the project was calculated
      assertThat(response).isNotNull();

      verify(projectRepository).save(any(Project.class));
    }
  }

  @Nested
  @DisplayName("Update Project Tests")
  class UpdateProjectTests {

    @Test
    @DisplayName("update project successfully as admin")
    void updateProjectSuccessfully() {

      Project project = createTestProject();
      UpdateProjectRequest request = createValidUpdateProjectRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(projectRepository.save(any(Project.class))).thenReturn(project);

      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember()));

      ProjectResponse response =
          projectService.updateProject(testProjectId, request, testWorkspaceMemberId);

      assertThat(response).isNotNull();
      assertThat(response.getName()).isEqualTo("Updated Project Name");

      verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when developer tries to update project")
    void throwExceptionWhenDeveloperUpdatesProject() {
      UpdateProjectRequest request = createValidUpdateProjectRequest();

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(createTestProject()));

      assertThatThrownBy(
              () -> projectService.updateProject(testProjectId, request, testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Only Admins, Managers, and Project Managers can update projects");

      verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when updating archived project")
    void throwExceptionWhenUpdatingArchivedProject() {
      Project archivedProject = createArchivedProject();
      UpdateProjectRequest request = createValidUpdateProjectRequest();

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(archivedProject));

      assertThatThrownBy(
              () -> projectService.updateProject(testProjectId, request, testWorkspaceMemberId))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Cannot update an archived project");

      verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("throw exception when project not found")
    void throwExceptionWhenProjectNotFound() {
      UpdateProjectRequest request = createValidUpdateProjectRequest();

      when(projectRepository.findById(testProjectId)).thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> projectService.updateProject(testProjectId, request, testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Project not found with id: " + testProjectId);

      verify(projectRepository, never()).save(any(Project.class));
    }
  }

  @Nested
  @DisplayName("Remove Member From Project Tests")
  class RemoveMemberFromProjectTests {

    @Test
    @DisplayName("remove member from project successfully")
    void removeMemberFromProject() {

      ProjectMember projectMember = createTestProjectMember();
      projectMember.setIsActive(true);

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.existsById(testProjectId)).thenReturn(true);

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(projectMember));

      projectService.removeMemberFromProject(testProjectId, testWorkspaceMemberId);

      assertThat(projectMember.getIsActive()).isFalse();
      assertThat(projectMember.getUpdatedAt()).isNotNull();

      verify(projectMemberRepository).save(projectMember);
    }

    @Test
    @DisplayName("throw exception when project not found")
    void throwExceptionWhenProjectNotFoundForRemoval() {

      when(projectRepository.existsById(testProjectId)).thenReturn(false);

      assertThatThrownBy(
              () -> projectService.removeMemberFromProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Project not found");
    }

    @Test
    @DisplayName("throw exception when unauthorized user tries to remove")
    void throwExceptionWhenUnauthorizedUserRemoves() {
      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.existsById(testProjectId)).thenReturn(true);

      when(securityUtils.isAdmin()).thenReturn(false);
      when(securityUtils.isManager()).thenReturn(false);
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember())); // not a project manager

      assertThatThrownBy(
              () -> projectService.removeMemberFromProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage(
              "Only Admins and Managers and Project Managers can remove members from projects");
    }

    @Test
    @DisplayName("throw exception when member not found on project")
    void throwExceptionWhenMemberNotFound() {

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.existsById(testProjectId)).thenReturn(true);

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(
              () -> projectService.removeMemberFromProject(testProjectId, testWorkspaceMemberId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessage("Member not found on this project");
    }
  }

  @Nested
  @DisplayName("Assign Member To Project Tests")
  class AssignMemberToProjectTests {

    @Test
    @DisplayName("assign member to project successfully")
    void assignMemberToProject() {

      Project project = createTestProject();
      WorkspaceMember workspaceMember = createTestWorkspaceMember();
      User user = createTestUser();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));

      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));
      when(securityUtils.isAdmin()).thenReturn(true);

      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(false);
      when(projectMemberRepository.save(any(ProjectMember.class)))
          .thenReturn(createTestProjectMember());
      when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

      ProjectMemberResponse response =
          projectService.assignMemberToProject(testProjectId, testWorkspaceMemberId, false);

      assertThat(response).isNotNull();
      assertThat(response.getFirstName()).isEqualTo("Test");
      assertThat(response.getLastName()).isEqualTo("User");
      assertThat(response.getIsProjectManager()).isFalse();

      verify(projectMemberRepository).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("throw exception when member already assigned to project")
    void throwExceptionWhenMemberAlreadyAssigned() {

      Project project = createTestProject();
      WorkspaceMember workspaceMember = createTestWorkspaceMember();

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      when(securityUtils.isAdmin()).thenReturn(true);
      when(projectMemberRepository.existsByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  projectService.assignMemberToProject(testProjectId, testWorkspaceMemberId, false))
          .isInstanceOf(StateConflictException.class)
          .hasMessage("Member is already assigned to this project");

      verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }

    @Test
    @DisplayName("throw exception when member does not belong to project workspace")
    void throwExceptionWhenMemberNotInWorkspace() {

      Project project = createTestProject();
      WorkspaceMember workspaceMember = createTestWorkspaceMember();
      workspaceMember.setWorkspaceId(UUID.randomUUID()); // Different workspace

      when(securityUtils.getDefaultWorkspaceMemberId()).thenReturn(testWorkspaceMemberId);
      when(projectRepository.findById(testProjectId)).thenReturn(Optional.of(project));
      when(workspaceMemberRepository.findById(testWorkspaceMemberId))
          .thenReturn(Optional.of(workspaceMember));

      assertThatThrownBy(
              () ->
                  projectService.assignMemberToProject(testProjectId, testWorkspaceMemberId, false))
          .isInstanceOf(AccessDeniedException.class)
          .hasMessage("Member does not belong to the project's workspace");

      verify(projectMemberRepository, never()).save(any(ProjectMember.class));
    }
  }
}
