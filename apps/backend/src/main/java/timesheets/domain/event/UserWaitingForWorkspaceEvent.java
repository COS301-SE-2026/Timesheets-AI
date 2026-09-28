package timesheets.domain.event;

import java.util.UUID;

public record UserWaitingForWorkspaceEvent(UUID userId) {}
