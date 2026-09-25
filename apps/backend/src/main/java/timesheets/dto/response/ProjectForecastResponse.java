package timesheets.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProjectForecastResponse(
    UUID projectId,
    String projectName,
    Budget budget,
    Schedule schedule,
    Tasks tasks,
    Velocity velocity,
    Risk risk,
    Confidence confidence,
    Map<String, Object> externalEvidence,
    AiExplanation aiExplanation) {

  public record Budget(
      Double budgetHours,
      double usedHours,
      Double remainingBudgetHours,
      Double forecastTotalHours,
      Double forecastOverrunHours) {}

  public record Schedule(
      LocalDate startDate,
      LocalDate plannedEndDate,
      LocalDate forecastEndDate,
      Integer delayDays,
      Double timelineProgressPercentage) {}

  public record Tasks(
      int totalTasks,
      int completedTasks,
      int remainingTasks,
      double completionPercentage,
      double estimatedRemainingHours) {}

  public record Velocity(
      double recentHoursPerWeek,
      int lookbackDays,
      double hoursInPeriod,
      boolean hasSufficientData) {}

  public record Risk(String budget, String schedule, String taskProgress, String overall) {}

  public record Confidence(
      String level,
      int evidenceAvailable,
      int evidenceTotal,
      List<String> missingEvidence,
      double taskEstimateCoveragePercentage) {}

  public record AiExplanation(
      String summary,
      String riskExplanation,
      List<String> contributingFactors,
      List<Recommendation> recommendations) {}

  public record Recommendation(String title, String description) {}
}
