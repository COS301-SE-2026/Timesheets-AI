/*
- this will use the Evidence Engine collectors that I have been given, instead of using the GitHub and Jira integrations directly
- for now I am not introducing calendar data because when people log their info alot of it might not be directly related to aproject
- also I don't think the info I can get from a calendar will be meaningful enough to contribute to project forecast you know?
*/

package timesheets.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import timesheets.domain.ProjectMember;
import timesheets.domain.Task;
import timesheets.dto.response.ProjectForecastEvidenceResponse;
import timesheets.dto.response.ProjectForecastEvidenceResponse.ActivityCount;
import timesheets.dto.response.ProjectForecastEvidenceResponse.ExternalEvidenceSummary;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.GitHubEvidenceCollector;
import timesheets.evidence.JiraEvidenceCollector;
import timesheets.repository.ProjectMemberRepository;
import timesheets.repository.TaskRepository;

@Service
@RequiredArgsConstructor
public class ProjectForecastEvidenceService {

  private final GitHubEvidenceCollector gitHubEvidenceCollector;
  private final JiraEvidenceCollector jiraEvidenceCollector;
  private final ProjectMemberRepository projectMemberRepository;
  private final TaskRepository taskRepository;

  /*
  - this is what will get the evidence of each member in a project
  - I used helpers to break down the function logic
   */
  public List<EvidenceEvent> collectProjectEvidence(
      UUID projectId, LocalDateTime startTime, LocalDateTime endTime) {

    List<ProjectMember> projectMembers =
        projectMemberRepository.findByProjectIdAndIsActiveTrue(projectId);
    List<EvidenceEvent> projectEvidence = new ArrayList<>();

    // collect evidence for every active member that belongs to the project
    for (ProjectMember projectMember : projectMembers) {

      UUID workspaceMemberId = projectMember.getWorkspaceMemberId();
      projectEvidence.addAll(collectExternalEvidence(workspaceMemberId, startTime, endTime));
    }

    /*
    - remember team members can be part of multiple projects
    - so I want to make sure that info only related to this project is what is kept
     */
    List<EvidenceEvent> filteredEvidence = new ArrayList<>();

    filteredEvidence.addAll(filterGitHubEvidence(projectEvidence, projectId));
    filteredEvidence.addAll(filterJiraEvidence(projectEvidence, projectId));

    return filteredEvidence;
  }

  // collects the GitHub evidence for a workspace member for the period of project forecast
  public List<EvidenceEvent> collectGitHubEvidence(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    try {
      return gitHubEvidenceCollector.collect(workspaceMemberId, startTime, endTime);
    } catch (RuntimeException exception) {
      return new ArrayList<>();
    }
  }

  // collects the Jira evidence for a workspace member for the period of project forecast
  public List<EvidenceEvent> collectJiraEvidence(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    try {
      return jiraEvidenceCollector.collect(workspaceMemberId, startTime, endTime);

    } catch (RuntimeException exception) {
      return new ArrayList<>();
    }
  }

  // want to combine the evidence into one list
  public List<EvidenceEvent> collectExternalEvidence(
      UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<EvidenceEvent> evidence = new ArrayList<>();

    evidence.addAll(collectGitHubEvidence(workspaceMemberId, startTime, endTime));
    evidence.addAll(collectJiraEvidence(workspaceMemberId, startTime, endTime));

    return evidence;
  }

  // filters the GitHub evidence
  private List<EvidenceEvent> filterGitHubEvidence(List<EvidenceEvent> evidence, UUID projectId) {

    return evidence.stream()
        .filter(event -> "GITHUB".equals(event.getSource()))
        .filter(event -> projectId.equals(event.getProjectId()))
        .toList();
  }

  // filters the Jira evidence
  private List<EvidenceEvent> filterJiraEvidence(List<EvidenceEvent> evidence, UUID projectId) {

    List<EvidenceEvent> jiraEvidence = new ArrayList<>();

    for (EvidenceEvent event : evidence) {

      if (!"JIRA".equals(event.getSource())) {
        continue;
      }

      // I am trying to handle that if the meta data is null, then I should handle that properly
      if (event.getMetadata() == null) {
        continue;
      }

      Object localTaskId = event.getMetadata().get("localTaskId");

      /*
      - if it does not have a local task link, then I cannot say that the Jira evidence, can safely belong to the project forecasting
      */
      if (localTaskId == null) {
        continue;
      }

      UUID taskId;

      try {
        taskId = UUID.fromString(localTaskId.toString());
      } catch (IllegalArgumentException exception) {
        continue;
      }

      Task task = taskRepository.findById(taskId).orElse(null);

      if (task != null && projectId.equals(task.getProjectId())) {
        jiraEvidence.add(event);
      }
    }

    return jiraEvidence;
  }

  /*
  - this prepares the response to sent to the ai-service
  - still keeping GitHuba and Jira separate so the forecast knows what exactly came from what */
  public ProjectForecastEvidenceResponse getProjectEvidence(
      UUID projectId, LocalDateTime startTime, LocalDateTime endTime) {

    List<EvidenceEvent> evidence = collectProjectEvidence(projectId, startTime, endTime);
    List<EvidenceEvent> githubEvidence =
        evidence.stream().filter(event -> "GITHUB".equals(event.getSource())).toList();
    List<EvidenceEvent> jiraEvidence =
        evidence.stream().filter(event -> "JIRA".equals(event.getSource())).toList();

    return ProjectForecastEvidenceResponse.builder()
        .projectId(projectId)
        .github(buildEvidenceSummary(githubEvidence))
        .jira(buildEvidenceSummary(jiraEvidence))
        .build();
  }

  /*
  - this will create a summary for an external evidence source
  - instead of the ai-service just seeing all the events
  - it just summarises whether evidence was found or not
  */
  private ExternalEvidenceSummary buildEvidenceSummary(List<EvidenceEvent> evidence) {

    // if no evidence was found then this source is not available
    if (evidence.isEmpty()) {
      return ExternalEvidenceSummary.builder()
          .available(false)
          .activityCount(0)
          .latestActivity(null)
          .activities(new ArrayList<>())
          .build();
    }

    // to count how many evidence event there are for each activity type
    Map<String, Long> activityCounts =
        evidence.stream()
            .filter(event -> event.getActivityType() != null)
            .collect(Collectors.groupingBy(EvidenceEvent::getActivityType, Collectors.counting()));

    List<ActivityCount> activities =
        activityCounts.entrySet().stream()
            .map(
                entry ->
                    ActivityCount.builder()
                        .activityType(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
            .toList();

    /*
    - to find the most recent activity from the evidence
    - its more so used as supporting, so that a whole picture on the evidence can be built
    - if the activity does not have a timestamp, then it is ignored
     */
    LocalDateTime latestActivity =
        evidence.stream()
            .map(EvidenceEvent::getTimestamp)
            .filter(timestamp -> timestamp != null)
            .max(LocalDateTime::compareTo)
            .orElse(null);

    return ExternalEvidenceSummary.builder()
        .available(true)
        .activityCount(evidence.size())
        .latestActivity(latestActivity)
        .activities(activities)
        .build();
  }
}
