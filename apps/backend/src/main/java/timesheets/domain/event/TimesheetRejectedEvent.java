package timesheets.domain.event;

import java.util.UUID;

public record TimesheetRejectedEvent(UUID timesheetId, UUID ownerWorkspaceMemberId) {}
