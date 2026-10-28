/*
this file handles the computes the deterministic confidence score for a whole Timesheet (a period, e.g. one week), aggregated across every TimeEntry inside it
only runs when ManagerAssistantController is hit directly, which only happens when the manager clicks "AI Review" on an opened timesheet. no scheduler, no
queue, no auto trigger anywhere in this file, dont add one later without checking this comment first

jira is read through JiraEvidenceCollector (timesheets.evidence), the same collector the project forecast uses, so both features agree on what a ticket in a
period is. github and calendar still go straight to their adapters/services

gemini never touches this class, only the one narrative sentence in ManagerAssistantNarrativeClient does, through ai-service. reasoning: a
confidence score gating a manager's approve/reject call needs to be testable and reproducible, cant write a junit test against an llm's output even at
temp 0, and the free gemini quota shouldn't be a single point of failure mid demo

weighting rationale, writing this down since the panel will def ask about the constants:
- github highest (0.5), a commit is a direct artifact of code changing,
  strongest signal the dev actually did the work
- jira medium (0.3), a ticket update proves something happened but looser
  timing than a commit timestamp
- calendar lowest (0.2), an event proves attendance not dev work

https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
for how the existing GitCommitRepository derived query works under the hood

Author: Zamokuhle Zwane
Date: 20 September 2026
*/

package timesheets.managerassistant;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import timesheets.domain.TimeEntry;
import timesheets.domain.Timesheet;
import timesheets.evidence.EvidenceEvent;
import timesheets.evidence.JiraEvidenceCollector;
import timesheets.integration.calendar.CalendarEvent;
import timesheets.integration.calendar.CalendarNotConnectedException;
import timesheets.integration.calendar.CalendarService;
import timesheets.integration.github.GitCommitActivity;
import timesheets.integration.github.GitHubAdapter;
import timesheets.integration.github.GitHubService;
import timesheets.repository.TimeEntryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagerAssistantScoringService {

  private static final double GITHUB_WEIGHT = 0.5;
  private static final double JIRA_WEIGHT = 0.3;
  private static final double CALENDAR_WEIGHT = 0.2;

  private static final int LIKELY_APPROVE_THRESHOLD = 75;
  private static final int LIKELY_REJECT_THRESHOLD = 45;

  private final GitHubAdapter gitHubAdapter;
  private final CalendarService CalendarService;
  private final JiraEvidenceCollector jiraEvidenceCollector;
  private final TimeEntryRepository timeEntryRepository;

  public ManagerAssistantReview scoreTimesheet(Timesheet timesheet) {
    UUID memberId = timesheet.getWorkspaceMemberId();

    // periodStart/periodEnd are LocalDate on the entity, converting to a full day window here, same
    // conversion CalendarController.getVsTracked
    // already does for this exact from/to to LocalDateTime problem
    LocalDateTime start = timesheet.getPeriodStart().atStartOfDay();
    LocalDateTime end = timesheet.getPeriodEnd().atTime(LocalTime.MAX);

    List<TimeEntry> entries = timeEntryRepository.findByTimesheetId(timesheet.getId());

    ManagerAssistantEvidenceResult github = scoreGithub(memberId, start, end);
    ManagerAssistantEvidenceResult jira = scoreJira(memberId, start, end);
    ManagerAssistantEvidenceResult calendar = scoreCalendar(memberId, start, end);

    double weighted =
        github.subScore() * GITHUB_WEIGHT
            + jira.subScore() * JIRA_WEIGHT
            + calendar.subScore() * CALENDAR_WEIGHT;

    int scorePercent = (int) Math.round(weighted * 100);

    String verdict =
        scorePercent >= LIKELY_APPROVE_THRESHOLD
            ? "LIKELY_APPROVE"
            : scorePercent <= LIKELY_REJECT_THRESHOLD ? "LIKELY_REJECT" : "NEEDS_REVIEW";

    boolean missingEvidence = anyEntryMissingEvidence(entries, github, jira, calendar);
    boolean conflict = anyEntryConflicts(entries, calendar);

    // log statement for anything flagged, so future me can trace WHY an entry landed in a certain
    // verdict when debugging this later
    if (missingEvidence) {
      log.info("timesheet {} flagged missing evidence, memberId={}", timesheet.getId(), memberId);
    }
    if (conflict) {
      log.info(
          "timesheet {} flagged conflict with calendar, memberId={}", timesheet.getId(), memberId);
    }

    return new ManagerAssistantReview(
        timesheet.getId(),
        scorePercent,
        verdict,
        List.of(github, jira, calendar),
        null, // ManagerAssistantNarrativeClient fills this in after this returns
        conflict,
        missingEvidence);
  }

  private final GitHubService gitHubService;

  private ManagerAssistantEvidenceResult scoreGithub(
      UUID memberId, LocalDateTime start, LocalDateTime end) {
    try {
      gitHubService.syncRecentCommits(memberId);
    } catch (IllegalStateException e) {
      // "GitHub is not connected", same treat-as-normal pattern as jira/calendar
      log.debug("github not connected for member {}, treating as 0 evidence", memberId);
      return new ManagerAssistantEvidenceResult("GITHUB", 0, 0.0, "github not connected");
    }

    // db backed via GitHubService, not a live api call, cheap to hit
    List<GitCommitActivity> commits = gitHubAdapter.getCommits(memberId, start, end);

    int count = commits.size();
    // caps at 1.0 once we hit 3 commits across the WHOLE period, not per
    // entry, three commits in a week is a reasonable "did real work" bar,
    // dont reward commit spam by scoring higher past that
    double subScore = Math.min(count / 3.0, 1.0);

    String detail =
        count == 0
            ? "0 commits (expected >= 1)"
            : count + " commit" + (count > 1 ? "s" : "") + " related to this period";

    return new ManagerAssistantEvidenceResult("GITHUB", count, subScore, detail);
  }

  private ManagerAssistantEvidenceResult scoreJira(
      UUID memberId, LocalDateTime start, LocalDateTime end) {
    // same ticket collection the project forecast runs: issues assigned to the member that were
    // created or updated inside the window, with Jira's timestamp format handled in JiraAdapter
    List<EvidenceEvent> events;
    try {
      events = jiraEvidenceCollector.collectIssueEvidence(memberId, start, end);
    } catch (RuntimeException e) {
      log.debug(
          "jira not connected or fetch failed for member {}, treating as 0 evidence", memberId, e);
      return new ManagerAssistantEvidenceResult("JIRA", 0, 0.0, "jira not connected");
    }

    long count =
        events.stream()
            .map(event -> event.getMetadata() == null ? null : event.getMetadata().get("issueKey"))
            .filter(Objects::nonNull)
            .distinct()
            .count();

    double subScore = count > 0 ? 1.0 : 0.0;

    String detail =
        count == 0
            ? "0 tickets (expected >= 1)"
            : count + " ticket" + (count > 1 ? "s" : "") + " with activity in this period";

    return new ManagerAssistantEvidenceResult("JIRA", (int) count, subScore, detail);
  }

  private ManagerAssistantEvidenceResult scoreCalendar(
      UUID memberId, LocalDateTime start, LocalDateTime end) {
    List<CalendarEvent> events;
    try {
      events = CalendarService.getEvents(memberId, start, end);
    } catch (CalendarNotConnectedException e) {
      return new ManagerAssistantEvidenceResult("CALENDAR", 0, 0.0, "calendar not connected");
    }

    int count = events.size();
    // lowest weight AND capped sub score on purpose, a meeting proves attendance not dev work, even
    // a match only gets 0.6, never full confidence on its own
    double subScore = count > 0 ? 0.6 : 0.0;

    String detail =
        count == 0
            ? "no calendar activity"
            : count + " event" + (count > 1 ? "s" : "") + " (meeting only)";

    return new ManagerAssistantEvidenceResult("CALENDAR", count, subScore, detail);
  }

  // spec case: if a dev logs "Research" with literally nothing corroborating is anywhere in the
  // period. this is a flag not an accusation, manager decides what it means
  private boolean anyEntryMissingEvidence(
      List<TimeEntry> entries,
      ManagerAssistantEvidenceResult github,
      ManagerAssistantEvidenceResult jira,
      ManagerAssistantEvidenceResult calendar) {
    boolean anyResearchEntry =
        entries.stream().anyMatch(e -> "RESEARCH".equalsIgnoreCase(e.getEntryType()));
    boolean noCorroboration =
        github.matchCount() == 0 && jira.matchCount() == 0 && calendar.matchCount() == 0;
    return !anyResearchEntry && noCorroboration;
  }

  // spec case: dev logs dev work but calendar shows a meeting overlapping it, doesn't mean anything
  // shady, could just be multitasking, but the
  // manager should see it before approving
  private boolean anyEntryConflicts(
      List<TimeEntry> entries, ManagerAssistantEvidenceResult calendar) {
    boolean anyDevEntry =
        entries.stream().anyMatch(e -> "DEVELOPMENT".equalsIgnoreCase(e.getEntryType()));
    return anyDevEntry && calendar.matchCount() > 0;
  }
}
