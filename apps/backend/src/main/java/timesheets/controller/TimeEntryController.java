package timesheets.controller;

import timesheets.domain.TimeEntry;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.dto.response.TimeEntryResponse;
import timesheets.enums.TimeEntryStatus;
import timesheets.service.TimeEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// the controller is the entry point for all HTTP requests from the frontend
// it receives requests, gives work to the service layer, and returns responses

@RestController //this makes it a REST API controller
@RequestMapping("/api/time-entries") //this will set the base URL such that all the methods start with that

public class TimeEntryController {
    
    private final TimeEntryService timeEntryService;
    
    public TimeEntryController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }
    
    // TODO: Replace with actual workspaceMemberId from JWT token after security is implemented
    // Using Bob's workspace_member_id from seed data
    private UUID getCurrentWorkspaceMemberId() {
        return UUID.fromString("00000000-0000-0000-0003-000000000002");
    }
    
    // This helper method converts a string ID from URL to UUID
    private UUID toUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid UUID format: " + id);
        }
    }

    @PostMapping //this is what with handle the HTTP POST requests ie. POST /api/time-entries
    // RequestBody will take the JSON from frontend and converts to TimeEntryRequest object
    // Valid this will trigger validation for example @NotNull, @Positive

    public ResponseEntity<TimeEntryResponse> createTimeEntry(@Valid @RequestBody TimeEntryRequest request) {

        UUID memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.createTimeEntry(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(TimeEntryResponse.from(entry));
    }
    // the above will convert the JSON from the frontend into a Java object
    // the @Valid triggers validation annotations in TimeEntryRequest
    


    @GetMapping("/me") //this will have the full URL GET /api/time-entries/me, it will handle the GET requests
    public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntries() {

        UUID memberId = getCurrentWorkspaceMemberId();
        List<TimeEntry> entries = timeEntryService.getMyTimeEntries(memberId);

        List<TimeEntryResponse> responses = entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    // the function above should return all the time entries for the logged-in user
    


    @GetMapping("/me/status/{status}")
    public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntriesByStatus(@PathVariable String status) {

        UUID memberId = getCurrentWorkspaceMemberId();
        TimeEntryStatus entryStatus = TimeEntryStatus.valueOf(status.toUpperCase());

        List<TimeEntry> entries = timeEntryService.getMyTimeEntriesByStatus(memberId, entryStatus);
        List<TimeEntryResponse> responses = entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    // this will return the enum values 
    


    @GetMapping("/{id}") 
    public ResponseEntity<TimeEntryResponse> getTimeEntryById(@PathVariable String id) {

        UUID entryId = toUUID(id);
        TimeEntry entry = timeEntryService.getTimeEntryById(entryId);
        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
    //this will return a specific entry by its ID
    


    @PutMapping("/{id}") //the full URL will be PUT /api/time-entries/1
    public ResponseEntity<TimeEntryResponse> updateTimeEntry(@PathVariable String id, @RequestBody TimeEntryRequest request) {
        
        UUID entryId = toUUID(id);
        UUID memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.updateTimeEntry(entryId, memberId, request);
        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
    //this will update an existing time entry only if the status is DRAFT
    


    @DeleteMapping("/{id}") //the full URL will be DELETE /api/time-entries/1
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable String id) {

        UUID entryId = toUUID(id);
        UUID memberId = getCurrentWorkspaceMemberId();
        timeEntryService.deleteTimeEntry(entryId, memberId);

        return ResponseEntity.noContent().build();
    }
    //this function will delete the requests
    

    @PostMapping("/{id}/submit")
    public ResponseEntity<TimeEntryResponse> submitTimeEntry(@PathVariable String id) {

        UUID entryId = toUUID(id);
        UUID memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.submitTimeEntry(entryId, memberId);

        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
}