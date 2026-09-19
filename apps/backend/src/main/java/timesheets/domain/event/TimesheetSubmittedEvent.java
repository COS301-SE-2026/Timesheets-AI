package timesheets.domain.event;

import java.util.UUID;

public record TimesheetSubmittedEvent(UUID timesheetId, UUID submittedByWorkspaceMemberId) {}
