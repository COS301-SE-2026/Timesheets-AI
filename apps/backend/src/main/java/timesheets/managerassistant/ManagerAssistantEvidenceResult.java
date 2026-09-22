/*
This file handles the shared shape for one evidence source's contribution to the manager
assistant score, github/jira/calendar all reuse this instead of three almost identical classes

Author: Zamokuhle Zwane
Date: 20 September 2026
*/

package timesheets.managerassistant;

public record ManagerAssistantEvidenceResult(
    String sourceName, // "GITHUB", "JIRA", "CALENDAR"
    int matchCount,
    double subScore, // 0.0 to 1.0, before weighting
    String detail) {}
