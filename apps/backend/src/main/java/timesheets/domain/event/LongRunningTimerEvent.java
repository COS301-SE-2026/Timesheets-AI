package timesheets.domain.event;

import java.util.UUID;

public record LongRunningTimerEvent(UUID timerId, UUID workspaceMemberId) {}
