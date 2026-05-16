package timesheets.service;

import timesheets.domain.TimeEntry;
import timesheets.dto.request.TimeEntryRequest;
import timesheets.enums.TimeEntryStatus;
import timesheets.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import exception.TimeEntryAccessDeniedException;
import exception.TimeEntryNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

//this is the file that has all my business logic, the control will call the service and the service will call repo

@Service
public class TimeEntryService {
    
    private final TimeEntryRepository timeEntryRepository;
    
    public TimeEntryService(TimeEntryRepository timeEntryRepository) {
        this.timeEntryRepository = timeEntryRepository;
    }
    
    @Transactional //this will help that is anything fails then the database rollback happens automatically
    public TimeEntry createTimeEntry(Long workspaceMemberId, TimeEntryRequest request) {

        TimeEntry entry = new TimeEntry();

        entry.setWorkspaceMemberId(workspaceMemberId);

        entry.setProjectId(request.getProjectId());
        entry.setTaskId(request.getTaskId());

        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setDurationMinutes(request.getDurationMinutes());
        entry.setEntryType(request.getEntryType());
        entry.setDescription(request.getDescription());

        entry.setStatus(TimeEntryStatus.DRAFT);
        
        return timeEntryRepository.save(entry);
    }
    
    public List<TimeEntry> getMyTimeEntries(Long workspaceMemberId) {
        return timeEntryRepository.findByWorkspaceMemberIdOrderByStartTimeDesc(workspaceMemberId);
    } 
    //all the entries should be gotten by this, also this will not need transactional because it's only reading data
    

    public List<TimeEntry> getMyTimeEntriesByStatus(Long workspaceMemberId, TimeEntryStatus status) {
        return timeEntryRepository.findByWorkspaceMemberIdAndStatus(workspaceMemberId, status);
    }
    //if we only want draft entries or only approved entries
    

    public TimeEntry getTimeEntryById(Long id) {
        return timeEntryRepository.findById(id).orElseThrow(() -> new TimeEntryNotFoundException(id));
    }
    //this will be if only one specific entry is needed

    @Transactional
    public TimeEntry updateTimeEntry(Long id, Long workspaceMemberId, TimeEntryRequest request) {
        TimeEntry entry = getTimeEntryById(id);
        
        //we only want draft entries to be editable, to maintain data integrity, we don't want users to go and edit entries that have already been added
        if (entry.getStatus() != TimeEntryStatus.DRAFT) {
            throw new RuntimeException("Cannot edit time entry that is already " + entry.getStatus().name());
        }
        
        //we only want the owner of time entries to be able to edit them
        if (!entry.getWorkspaceMemberId().equals(workspaceMemberId)) {
            throw new RuntimeException("You can only edit your own time entries");
        }
        
        entry.setProjectId(request.getProjectId());
        entry.setTaskId(request.getTaskId());

        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());

        entry.setDurationMinutes(request.getDurationMinutes());
        entry.setEntryType(request.getEntryType());
        entry.setDescription(request.getDescription());
        entry.setEditedAt(LocalDateTime.now());

        entry.setEditedByWorkspaceMemberId(workspaceMemberId);
        
        return timeEntryRepository.save(entry);
    }

    
    

    @Transactional
    public void deleteTimeEntry(Long id, Long workspaceMemberId) {

        TimeEntry entry = getTimeEntryById(id);
        
        if (entry.getStatus() != TimeEntryStatus.DRAFT) {
            throw new RuntimeException("Cannot delete time entry that is already " + entry.getStatus().name());
        }
        
        if (!entry.getWorkspaceMemberId().equals(workspaceMemberId)) {
            throw new TimeEntryAccessDeniedException("You can only delete your own time entries");
        }
        
        timeEntryRepository.deleteById(id);
    }
    
    //I am doing these things to maintain data integrity
    @Transactional
    public TimeEntry submitTimeEntry(Long id, Long workspaceMemberId) {
        TimeEntry entry = getTimeEntryById(id);
        
        if (entry.getStatus() != TimeEntryStatus.DRAFT) {
            throw new RuntimeException("Time entry has already been submitted");
        }
        
        if (!entry.getWorkspaceMemberId().equals(workspaceMemberId)) {
            throw new RuntimeException("You can only submit your own time entries");
        }
        
        entry.setStatus(TimeEntryStatus.SUBMITTED);
        entry.setSubmittedAt(LocalDateTime.now());
        
        return timeEntryRepository.save(entry);
    }
}