package timesheets.integration.issue;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import timesheets.dto.IssueDto;

@Service 
public class JiraAdapter implements IssueTrackerAdapter {
    @Override 
    public String getProvider(){
        return "JIRA";
    }

    @Override
    public List<IssureDto> getIssues(UUID workspaceMemberId){
        return null;
    }

    @Override 
    public IssueDTO getIssue(UUID workspaceMemberId, String issueKey){
        return null;
    }
}