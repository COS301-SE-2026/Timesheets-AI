export interface SuggestedWorkSession {
    id: string;
    workspaceMemberId: string;
    startTime: string;
    endTime: string;
    title: string;
    projectId: string | null;
    taskId: string | null;
    confidenceScore: number;
    durationMinutes: number;
    explanation: string;
    status: 'PENDING' | 'EDITED' | 'APPROVED' | 'REJECTED';
    evidenceEvents: EvidenceEvent[];
}

export interface EvidenceEvent {
    id: string;
    source: string;
    timestamp: string;
    endTime?: string;
    projectId?: string;
    workspaceMemberId: string;
    taskId?: string;
    activityType: string;
    description: string;
    metadata?: { [key: string]: any;};
}

export interface EditSuggestionRequest {
    title: string;
    startTime: string;
    endTime: string;
    projectId: string;
    taskId: string;
    description: string;
}