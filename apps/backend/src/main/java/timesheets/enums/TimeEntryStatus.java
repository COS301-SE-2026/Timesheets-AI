package timesheets.enums;


public enum TimeEntryStatus {
    
    DRAFT("draft", "Time entry is being edited, not yet submitted"),
    SUBMITTED("submitted", "Time entry submitted for approval"),
    APPROVED("approved", "Time entry approved by manager"),
    REJECTED("rejected", "Time entry rejected by manager");
    
    private final String code;
    private final String description;
    
    TimeEntryStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static TimeEntryStatus fromCode(String code) {
        TimeEntryStatus[] statuses = TimeEntryStatus.values();

        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].getCode().equals(code)) {
                return statuses[i];
            }
        }

        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}