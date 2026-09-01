import { injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';

export type WorkspaceRole = 'ADMIN' | 'MANAGER' | 'DEVELOPER';

export interface AvailableTeamUser {
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    isInWorkspace: boolean;
}

export interface WorkspaceMemberResponse {
    workspaceMemberId: string;
    firstName: string;
    lastName: string;
    email: string;
    role: WorkspaceRole;
    joinedAt: string;
}

export interface ProjectMemberResponse {
    projectMemberId: string;
    workspaceMemmberId: string;
    userId: string;
    firstName: string;
    lastName: string;
    email: string;
    isProjectManager: boolean;
    joinedAt: string;
}

@Injectable({ providedIn: 'root' })
export class TeamService {
    private readonly http = inject(HttpClient);
    private readonly teamUrl = '/api/teams';
    private readonly projectUrl = '/api/projects';

    // fetch available users
    getAvailableUsers(): Observable<AvailableTeamUser[]> {
        return this.http
            .get<AvailableTeamUser[]>(`${this.teamUrl}/members/available`)
            .pipe(catchError(this.handleError('getAvailableUsers')));
    }

    // add users to workspace
    addToWorkspace(userId: string, role: WorkspaceRole = 'DEVELOPER'): Observable<WorkspaceMemberResponse> {
        return this.http
            .post<WorkspaceMemberResponse>(`${this.teamUrl}/members`, {userId, role })
            .pipe(catchError(this.handleError('assToWorksoace')));
    }

    // Remove user from workspace 
    removeFromWorkspace(workspaceMemberId: string): Observable<void> {
        return this.http 
            .delete<void>(`${this.teamUrl}/members/${workspaceMemberId}`)
            .pipe(catchError(this.handleError('removeFromWorkspace')));
    }


}