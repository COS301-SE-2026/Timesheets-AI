package timesheets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import timesheets.dto.response.ProjectDetailResponse;
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
}
