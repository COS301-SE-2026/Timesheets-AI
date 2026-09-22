/*
this file handles the the ONE response shape the manager assistant endpoint returns,
everything the evidence review modal needs in a single call
named timesheetId not timeEntryId, caught the mismatch mid build, this scores
a whole timesheet period now not one entry, see ManagerAssistantScoringService
for why

Author: Zamokuhle Zwane
Date: 20 September 2026
*/

package timesheets.managerassistant;

import java.util.List;
import java.util.UUID;

public record ManagerAssistantReview(
    UUID timesheetId,
    int confidenceScorePercent,
    String verdict, // LIKELY_APPROVE, LIKELY_REJECT, NEEDS_REVIEW
    List<ManagerAssistantEvidenceResult> evidenceSources,
    String narrative,
    boolean hasConflict,
    boolean hasMissingEvidence) {}
