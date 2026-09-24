// resembles what is in the backend 
// Represents the backend SuggestedWorkSession

export interface SuggestedWorkSession {
    id: string;
    workspaceMemberId: string;
    startTime: string;
    endTime: string;
    title: string;
    description: string;
    projectId: string | null;
    taskId: string | null;
    evidenceEvents: EvidenceEvent[];
    confidenceScore: number;
    durationMinutes: number;
    explanation: string;
    status: SuggestionStatus;
}

export interface EvidenceEvent {
    id: string;
    source: string;
    timestamp: string;
    projectId: string | null;
    workspaceMemberId: string;
    taskId: string | null;
    activityType: string;
    description: string;
    endTime: string | null;
    metadata: { [key: string]: any};
}

export type SuggestionStatus = 'PENDING' | 'EDITED' | 'APPROVED' | 'REJECTED';