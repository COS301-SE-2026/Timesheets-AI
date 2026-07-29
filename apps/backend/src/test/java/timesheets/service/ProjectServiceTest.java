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
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.dto.request.CreateProjectRequest;
import timesheets.dto.response.ProjectResponse;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.security.SecurityUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectService Unit Tests")
public class ProjectServiceTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ProjectService projectService;

  private final UUID testProjectId = UUID.randomUUID();
  private final UUID testWorkspaceId = UUID.randomUUID();
  private final String testProjectName = "Test Project";
  private final String testProjectDescription = "Test Description";
  private final BigDecimal testBudgetHours = BigDecimal.valueOf(100);
  private final BigDecimal testHourlyRate = BigDecimal.valueOf(50);
  private final BigDecimal testBudgetCost = BigDecimal.valueOf(5000);
  private final UUID testWorkspaceMemberId = UUID.randomUUID();

  // ! helper functions
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

      // to find the user role in the project
      when(projectMemberRepository.findByProjectIdAndWorkspaceMemberId(
              testProjectId, testWorkspaceMemberId))
          .thenReturn(Optional.of(createTestProjectMember()));

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
}
