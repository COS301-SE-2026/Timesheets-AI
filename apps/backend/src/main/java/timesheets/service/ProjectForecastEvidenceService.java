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

    List<ProjectMember> projectMembers = projectMemberRepository.findByProjectIdAndIsActiveTrue(projectId);
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

  //collects the GitHub evidence for a workspace member for the period of project forecast
  public List<EvidenceEvent> collectGitHubEvidence(UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    return gitHubEvidenceCollector.collect(workspaceMemberId, startTime, endTime);
  }

    //collects the Jira evidence for a workspace member for the period of project forecast
  public List<EvidenceEvent> collectJiraEvidence(UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    return jiraEvidenceCollector.collect(workspaceMemberId, startTime, endTime);
  }

  
  //want to combine the evidence into one list
  public List<EvidenceEvent> collectExternalEvidence(UUID workspaceMemberId, LocalDateTime startTime, LocalDateTime endTime) {

    List<EvidenceEvent> evidence = new ArrayList<>();

    evidence.addAll(collectGitHubEvidence(workspaceMemberId, startTime, endTime));
    evidence.addAll(collectJiraEvidence(workspaceMemberId, startTime, endTime));

    return evidence;
  }


  //filters the GitHub evidence
  private List<EvidenceEvent> filterGitHubEvidence(List<EvidenceEvent> evidence, UUID projectId) {

    return evidence.stream().filter(event -> "GITHUB".equals(event.getSource())).filter(event -> projectId.equals(event.getProjectId())).toList();
  }

  //filters the Jira evidence
  private List<EvidenceEvent> filterJiraEvidence(List<EvidenceEvent> evidence, UUID projectId) {

    List<EvidenceEvent> jiraEvidence = new ArrayList<>();

    for (EvidenceEvent event : evidence) {

      if (!"JIRA".equals(event.getSource())) {
        continue;
      }

      //I am trying to handle that if the meta data is null, then I should handle that properly
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
      } 
      catch (IllegalArgumentException exception) {
        continue;
      }

      Task task = taskRepository.findById(taskId).orElse(null);

      if (task != null && projectId.equals(task.getProjectId())) {
        jiraEvidence.add(event);
      }
    }

    return jiraEvidence;
  }

  
}
