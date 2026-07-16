package timesheets.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import timesheets.domain.TimeEntry;
import timesheets.domain.User;
import timesheets.domain.WorkspaceMember;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.repository.UserRepository;
import timesheets.repository.WorkspaceMemberRepository;
import timesheets.service.TimeEntryService;

// the controller is the entry point for all HTTP requests from the frontend
// it receives requests, gives work to the service layer, and returns responses

@RestController // this makes it a REST API controller
// this will set the base URL such that all the methods start with that
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

  private final TimeEntryService timeEntryService;
  private final WorkspaceMemberRepository workspaceMemberRepository;
  private final UserRepository userRepository;

  private UUID getCurrentWorkspaceMemberId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    // gets the Spring Security User
    org.springframework.security.core.userdetails.User springUser =
        (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

    // gets email from Spring Security User
    String email = springUser.getUsername();

    // finds your custom user from database
    User user =
        userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

    // gets workspace member
    return workspaceMemberRepository.findByUserId(user.getId()).stream()
        .findFirst()
        .map(WorkspaceMember::getId)
        .orElseThrow(() -> new RuntimeException("User is not a member of any workspace"));
  }

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

    UUID memberId = getCurrentWorkspaceMemberId();
    TimeEntry entry = timeEntryService.createTimeEntry(memberId, request);

    return ResponseEntity.status(HttpStatus.CREATED).body(TimeEntryResponse.from(entry));
  }

  // this will have the full URL GET /api/time-entries/me, it will handle the GET requests
  // gets all the time entries of an authenticated user
  @GetMapping("/me")
  public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntries() {

    UUID memberId = getCurrentWorkspaceMemberId();

    List<TimeEntry> entries = timeEntryService.getMyTimeEntries(memberId);
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

  // this will edit an entry if it is not locked
  @PutMapping("/{id}")
  public ResponseEntity<TimeEntryResponse> updateTimeEntry(
      @PathVariable String id, @RequestBody TimeEntryRequest request) {

    UUID entryId = toUUID(id);
    UUID memberId = getCurrentWorkspaceMemberId();

    TimeEntry entry = timeEntryService.updateTimeEntry(entryId, memberId, request);

    return ResponseEntity.ok(TimeEntryResponse.from(entry));
  }

  // this will create a soft delete if the time entry is not locked
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTimeEntry(@PathVariable String id) {

    UUID entryId = toUUID(id);
    UUID memberId = getCurrentWorkspaceMemberId();

    timeEntryService.deleteTimeEntry(entryId, memberId);

    return ResponseEntity.noContent().build();
  }
}
