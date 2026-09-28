package timesheets.domain.event;

import java.util.UUID;

public record TimesheetApprovedEvent(UUID timesheetId, UUID ownerWorkspaceMemberId) {}
