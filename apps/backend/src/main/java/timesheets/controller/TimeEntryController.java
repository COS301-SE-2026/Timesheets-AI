package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import timesheets.domain.TimeEntry;
import timesheets.dto.request.TimeEntryPatchRequest;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.security.SecurityUtils;
import timesheets.service.TimeEntryService;

// the controller is the entry point for all HTTP requests from the frontend
// it receives requests, gives work to the service layer, and returns responses

@RestController // this makes it a REST API controller
// this will set the base URL such that all the methods start with that
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

  private final TimeEntryService timeEntryService;
  private final SecurityUtils securityUtils;

  // helper converts a string ID from URL to UUID
  private UUID toUUID(String id) {
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid UUID format: " + id);
    }
  }

  // this is what with handle the HTTP POST requests ie. POST /api/time-entries
  // RequestBody will take the JSON from frontend and converts to TimeEntryRequest object
  // Valid this will trigger validation for example @NotNull, @Positive
  // create a new time entry for the authenticated user
  @PostMapping
  public ResponseEntity<TimeEntryResponse> createTimeEntry(
      @Valid @RequestBody TimeEntryRequest request) {

    TimeEntry entry = timeEntryService.createTimeEntry(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(TimeEntryResponse.from(entry));
  }

  // this will have the full URL GET /api/time-entries/me, it will handle the GET requests
  // gets all the time entries of an authenticated user
  @GetMapping("/me")
  public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntries() {

    List<TimeEntry> entries = timeEntryService.getMyTimeEntries();
    List<TimeEntryResponse> responses =
        entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());

    return ResponseEntity.ok(responses);
  }

  // this will get a single entry by the ID
  @GetMapping("/{id}")
  public ResponseEntity<TimeEntryResponse> getTimeEntryById(@PathVariable String id) {

    UUID entryId = toUUID(id);
    TimeEntry entry = timeEntryService.getTimeEntryById(entryId);

    return ResponseEntity.ok(TimeEntryResponse.from(entry));
  }

  // this will edit the entrire time entry if it is not locked
  @PutMapping("/{id}")
  public ResponseEntity<TimeEntryResponse> updateTimeEntry(
      @PathVariable String id, @RequestBody TimeEntryRequest request) {

    UUID entryId = toUUID(id);

    TimeEntry entry = timeEntryService.updateTimeEntry(entryId, request);

    return ResponseEntity.ok(TimeEntryResponse.from(entry));
  }

  // this will allow a partial update of the time entry
  @PatchMapping("/{id}")
  public ResponseEntity<TimeEntryResponse> patchTimeEntry(
      @PathVariable String id, @RequestBody TimeEntryPatchRequest request) {
    UUID entryId = toUUID(id);
    TimeEntry entry = timeEntryService.updateTimeEntryPatch(entryId, request);
    return ResponseEntity.ok(TimeEntryResponse.from(entry));
  }

  // this will create a soft delete if the time entry is not locked
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTimeEntry(@PathVariable String id) {

    UUID entryId = toUUID(id);

    timeEntryService.deleteTimeEntry(entryId);

    return ResponseEntity.noContent().build();
  }
}
