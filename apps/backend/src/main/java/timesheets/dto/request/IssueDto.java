package timesheets.dto;

public class IssueDto {

  private String key;
  private String summary;
  private String status;
  private String issueType;

  public IssueDto() {}

  public IssueDto(String key, String summary, String status, String issueType) {
    this.key = key;
    this.summary = summary;
    this.status = status;
    this.issueType = issueType;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getIssueType() {
    return issueType;
  }

  public void setIssueType(String issueType) {
    this.issueType = issueType;
  }
}
