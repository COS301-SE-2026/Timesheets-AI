// Author: Cleopatra K
// Date: 2026-09-17
// Purpose: frontened will use this to determine the pills connection
// especially for which provider is connected
package timesheets.integration.calendar;

public record  CalendarStatus (
    boolean connected,
    String provider,
    String lastSyncedAt
){}
