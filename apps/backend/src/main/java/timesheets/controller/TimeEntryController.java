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
    
    // TODO: Replace 1L with actual workspaceMemberId from JWT token after security is implemented
    private Long getCurrentWorkspaceMemberId() {
        return 1L;
    }
    

    @PostMapping //this is what with handle the HTTP POST requests ie. POST /api/time-entrie
    // RequestBody will take the JSON from frontend and converts to TimeEntryRequest object
    // Valid this will trigger validation for example @NotNull, @Positive

    public ResponseEntity<TimeEntryResponse> createTimeEntry(@Valid @RequestBody TimeEntryRequest request) {

        Long memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.createTimeEntry(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(TimeEntryResponse.from(entry));
    }
    // the above will convert the JSON from the frontend into a Java object
    // the @Valid triggers validation annotations in TimeEntryRequest
    


    @GetMapping("/me") //this will have the full URL GET /api/time-entries/me, it will handle the GET requests
    public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntries() {

        Long memberId = getCurrentWorkspaceMemberId();
        List<TimeEntry> entries = timeEntryService.getMyTimeEntries(memberId);

        List<TimeEntryResponse> responses = entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    // the function above should return all the time entries for the logged-in user
    


    @GetMapping("/me/status/{status}")
    public ResponseEntity<List<TimeEntryResponse>> getMyTimeEntriesByStatus(@PathVariable String status) {

        Long memberId = getCurrentWorkspaceMemberId();
        TimeEntryStatus entryStatus = TimeEntryStatus.valueOf(status.toUpperCase());

        List<TimeEntry> entries = timeEntryService.getMyTimeEntriesByStatus(memberId, entryStatus);
        List<TimeEntryResponse> responses = entries.stream().map(TimeEntryResponse::from).collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
    // this will return the enum values 
    


    @GetMapping("/{id}") 
    public ResponseEntity<TimeEntryResponse> getTimeEntryById(@PathVariable Long id) {

        TimeEntry entry = timeEntryService.getTimeEntryById(id);
        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
    //this will return a specific entry by its ID
    



    @PutMapping("/{id}") //the full URL will be PUT /api/time-entries/1
    public ResponseEntity<TimeEntryResponse> updateTimeEntry(@PathVariable Long id, @RequestBody TimeEntryRequest request) {
        Long memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.updateTimeEntry(id, memberId, request);
        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
    //this will update an existing time entry only if the status is DRAFT
    



    @DeleteMapping("/{id}") //the full URL will be DELETE /api/time-entries/1
    public ResponseEntity<Void> deleteTimeEntry(@PathVariable Long id) {

        Long memberId = getCurrentWorkspaceMemberId();
        timeEntryService.deleteTimeEntry(id, memberId);

        return ResponseEntity.noContent().build();
    }
    //this function will delete the requests
    

    

    @PostMapping("/{id}/submit")
    public ResponseEntity<TimeEntryResponse> submitTimeEntry(@PathVariable Long id) {

        Long memberId = getCurrentWorkspaceMemberId();
        TimeEntry entry = timeEntryService.submitTimeEntry(id, memberId);

        return ResponseEntity.ok(TimeEntryResponse.from(entry));
    }
}