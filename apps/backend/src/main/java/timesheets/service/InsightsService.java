package timesheets.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.client.AiServiceClient;
import timesheets.domain.JiraTicket;
import timesheets.domain.Project;
import timesheets.domain.ProjectMember;
import timesheets.domain.TimeEntry;
import timesheets.dto.request.ProductivityReportRequest;
import timesheets.dto.response.AiDashboardResponse;
import timesheets.dto.response.DeveloperProjectResponse;
import timesheets.dto.response.GenerateInsightsResponse;
import timesheets.dto.response.IssueResponse;
import timesheets.dto.response.ManagerDashboardResponse;
import timesheets.dto.response.PersonalInsightsResponse;
import timesheets.dto.response.ResolveInsightResponse;
import timesheets.dto.response.WeeklySummaryResponse;
import timesheets.integration.github.GitHubService;
import timesheets.integration.issue.JiraAdapter;
import timesheets.repository.IntegrationTokenRepository;
import timesheets.repository.JiraTicketRepository;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.ProjectRepository;
import timesheets.repository.TaskRepository;
import timesheets.repository.TimeEntryRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.security.SecurityUtils;

// service for generating insights and analytics based on time entries,
// currently implements a summary report with various metrics and breakdowns
// these are currently for just the developer's own time entries, but could be extended to include
// team-level insights in the future after demo 1
@Service
@RequiredArgsConstructor
public class InsightsService {

  private final TimeEntryRepository timeEntryRepository;
  private final SecurityUtils securityUtils;
  private final AiServiceClient aiServiceClient;
  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final GitHubService gitHubService;
  private final JiraAdapter jiraAdapter;
  private final IntegrationTokenRepository integrationTokenRepository;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final TaskRepository taskRepository;
  private final JiraTicketRepository jiraTicketRepository;

  public PersonalInsightsResponse getInsightsSummary(ProductivityReportRequest request) {

    // get user ID from the security context
    UUID userId = securityUtils.getCurrentUserId();

    // fetch all time entries for the developer in the date range
    List<TimeEntry> entries =
        timeEntryRepository.findByUserIdAndDateRange(
            userId,
            request.getFrom().atStartOfDay(),
            request.getTo().atTime(23, 59, 59) // include entire end day
            );

    // total hours logged
    double totalHours =
        entries.stream().mapToDouble(entry -> entry.getDurationSeconds() / 3600.0).sum();

    // count unique days with entries
    long uniqueDays =
        entries.stream().map(entry -> entry.getStartTime().toLocalDate()).distinct().count();

    // average hours per day
    long daysBetween = ChronoUnit.DAYS.between(request.getFrom(), request.getTo()) + 1;
    double avgHoursPerDay = daysBetween > 0 ? totalHours / daysBetween : 0;

    // hours per day breakdown
    // Map<LocalDate, Double> hoursPerDay = entries.stream()
    // .collect(Collectors.groupingBy(
    //         entry -> entry.getStartTime().toLocalDate(),
    //         Collectors.summingDouble(entry -> entry.getDurationSeconds() / 60.0)
    // ));

    Map<UUID, String> projectNames =
        projectRepository
            .findAllById(entries.stream().map(TimeEntry::getProjectId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(Project::getId, Project::getName));

    // hours per project
    List<PersonalInsightsResponse.ProjectHours> hoursPerProject =
        entries.stream().collect(Collectors.groupingBy(TimeEntry::getProjectId)).entrySet().stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.ProjectHours.builder()
                      .projectId(entry.getKey())
                      .projectName(projectNames.getOrDefault(entry.getKey(), "Unknown project"))
                      .hours(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
            .collect(Collectors.toList());

    // hours per task
    List<PersonalInsightsResponse.TaskHours> hoursPerTask =
        entries.stream()
            .filter(entry -> entry.getTaskId() != null)
            .collect(Collectors.groupingBy(TimeEntry::getTaskId))
            .entrySet()
            .stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.TaskHours.builder()
                      .taskId(entry.getKey())
                      .taskTitle("Task " + entry.getKey())
                      .hours(hours)
                      .status("TODO")
                      .build();
                })
            .sorted((a, b) -> Double.compare(b.getHours(), a.getHours()))
            .limit(10)
            .collect(Collectors.toList());

    // daily trend breakdown for line chart, group by date and sum hours
    Map<LocalDate, List<TimeEntry>> dailyGroups =
        entries.stream()
            .collect(Collectors.groupingBy(entry -> entry.getStartTime().toLocalDate()));

    List<PersonalInsightsResponse.DailyTrend> dailyTrend =
        dailyGroups.entrySet().stream()
            .map(
                entry -> {
                  double hours =
                      entry.getValue().stream()
                          .mapToDouble(e -> e.getDurationSeconds() / 3600.0)
                          .sum();

                  return PersonalInsightsResponse.DailyTrend.builder()
                      .date(entry.getKey().format(DateTimeFormatter.ISO_DATE))
                      .hours(hours)
                      .entryCount(entry.getValue().size())
                      .build();
                })
            .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
            .collect(Collectors.toList());
    // build and return response
    return PersonalInsightsResponse.builder()
        .totalHoursLogged(totalHours)
        .averageHoursPerDay(avgHoursPerDay)
        .totalDaysLogged((int) uniqueDays)
        // .hoursPerDay(hoursPerDay)
        .hoursPerProject(hoursPerProject)
        .hoursPerTask(hoursPerTask)
        .dailyTrend(dailyTrend)
        .build();
  }

  public AiDashboardResponse getAiDashboard() {
    return getAiDashboard(false, "8w");
  }

  public AiDashboardResponse getAiDashboard(boolean includeResolved, String period) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    return aiServiceClient.getDashboardInsights(workspaceMemberId, includeResolved, period);
  }

  public ResolveInsightResponse resolveInsight(UUID insightId) {
    UUID resolvedBy = securityUtils.getDefaultWorkspaceMemberId();
    return aiServiceClient.resolveInsight(insightId, resolvedBy);
  }

  // every project this dev belongs to, active or not, with
  // all-time hours logged per project, a standing list, not scoped to a period
  public List<DeveloperProjectResponse> getMyProjects() {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();

    List<ProjectMember> memberships =
        projectMemberRepository.findByWorkspaceMemberId(workspaceMemberId);
    if (memberships.isEmpty()) {
      return List.of();
    }

    List<UUID> projectIds =
        memberships.stream().map(ProjectMember::getProjectId).distinct().toList();
    Map<UUID, String> projectNames =
        projectRepository.findAllById(projectIds).stream()
            .collect(Collectors.toMap(Project::getId, Project::getName));

    List<TimeEntry> allEntries = timeEntryRepository.findByWorkspaceMemberId(workspaceMemberId);
    Map<UUID, Double> hoursByProject =
        allEntries.stream()
            .collect(
                Collectors.groupingBy(
                    TimeEntry::getProjectId,
                    Collectors.summingDouble(e -> e.getDurationSeconds() / 3600.0)));

    return memberships.stream()
        .map(
            membership ->
                DeveloperProjectResponse.builder()
                    .projectId(membership.getProjectId())
                    .projectName(
                        projectNames.getOrDefault(membership.getProjectId(), "Unknown project"))
                    .hoursLogged(hoursByProject.getOrDefault(membership.getProjectId(), 0.0))
                    .active(Boolean.TRUE.equals(membership.getIsActive()))
                    .build())
        .toList();
  }

  // manager insights dashboard, delegates the actual scoping/aggregation to
  // ai-service, this method's only job is figuring out WHO is asking
  public ManagerDashboardResponse getManagerDashboard(UUID projectId, String period) {
    UUID workspaceMemberId = securityUtils.getDefaultWorkspaceMemberId();
    ManagerDashboardResponse response =
        aiServiceClient.getManagerDashboard(workspaceMemberId, projectId, period);

    if (response.getFlaggedBurnoutMembers() != null) {
      for (ManagerDashboardResponse.FlaggedBurnoutMember flagged :
          response.getFlaggedBurnoutMembers()) {
        var member = workspaceMemberRepository.findById(flagged.getWorkspaceMemberId());
        if (member.isPresent()) {
          var wm = member.get();
          flagged.setMemberRole(wm.getRole().toString());
        }
      }
    }
    return response;
  }

  @org.springframework.transaction.annotation.Transactional
  public GenerateInsightsResponse generateInsights(UUID projectId) {
    List<ProjectMember> members = projectMemberRepository.findByProjectIdAndIsActiveTrue(projectId);

    GenerateInsightsResponse.SyncResult githubResult = syncGithubForProject(members);
    GenerateInsightsResponse.SyncResult jiraResult = syncJiraForProject(projectId, members);

    return new GenerateInsightsResponse(githubResult, jiraResult);
  }

  public WeeklySummaryResponse getTeamWeeklySummary(java.time.LocalDate weekStart) {
    UUID workspaceId = securityUtils.getCurrentWorkspaceId();
    java.time.LocalDate resolvedWeekStart = weekStart != null ? weekStart : currentWeekMonday();
    return aiServiceClient.generateWeeklySummary(workspaceId, "TEAM", resolvedWeekStart);
  }

  private java.time.LocalDate currentWeekMonday() {
    java.time.LocalDate today = java.time.LocalDate.now();
    return today.minusDays(today.getDayOfWeek().getValue() - 1L);
  }

  private GenerateInsightsResponse.SyncResult syncGithubForProject(List<ProjectMember> members) {
    boolean anySucceeded = false;
    for (ProjectMember member : members) {
      try {
        if (gitHubService.isConnected(member.getWorkspaceMemberId())) {
          gitHubService.syncRecentCommits(member.getWorkspaceMemberId());
          anySucceeded = true;
        }
      } catch (Exception e) {
      }
    }
    return new GenerateInsightsResponse.SyncResult(
        anySucceeded ? "LIVE" : "CACHED", LocalDateTime.now());
  }

  private GenerateInsightsResponse.SyncResult syncJiraForProject(
      UUID projectId, List<ProjectMember> members) {
    boolean anySucceeded = false;

    for (ProjectMember member : members) {
      boolean isConnected =
          integrationTokenRepository
              .findByWorkspaceMemberIdAndProvider(member.getWorkspaceMemberId(), "JIRA")
              .isPresent();
      if (!isConnected) {
        continue;
      }

      try {
        List<IssueResponse> issues = jiraAdapter.getIssues(member.getWorkspaceMemberId());

        for (IssueResponse issue : issues) {
          taskRepository
              .findByJiraTicketKey(issue.getKey())
              .filter(task -> task.getProjectId().equals(projectId))
              .ifPresent(task -> upsertJiraTicket(projectId, issue));
        }
        anySucceeded = true;
      } catch (Exception e) {
        // per-integration, per-member catch, same reasoning as the github loop above
      }
    }

    return new GenerateInsightsResponse.SyncResult(
        anySucceeded ? "LIVE" : "CACHED", LocalDateTime.now());
  }

  private void upsertJiraTicket(UUID projectId, IssueResponse issue) {
    JiraTicket ticket =
        jiraTicketRepository.findByJiraTicketKeyIn(List.of(issue.getKey())).stream()
            .findFirst()
            .orElseGet(
                () -> {
                  JiraTicket newTicket = new JiraTicket();
                  newTicket.setId(java.util.UUID.randomUUID());
                  newTicket.setJiraTicketKey(issue.getKey());
                  newTicket.setCreatedAt(LocalDateTime.now());
                  return newTicket;
                });

    ticket.setProjectId(projectId);
    ticket.setSummary(issue.getSummary());
    ticket.setJiraStatus(issue.getStatus());
    ticket.setIssueType(issue.getIssueType());
    ticket.setLastSynced(LocalDateTime.now());

    jiraTicketRepository.save(ticket);
  }
}
