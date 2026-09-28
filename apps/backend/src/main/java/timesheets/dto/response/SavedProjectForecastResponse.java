package timesheets.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SavedProjectForecastResponse(
    UUID projectId, LocalDateTime lastSyncedAt, ProjectForecastResponse forecast) {}
