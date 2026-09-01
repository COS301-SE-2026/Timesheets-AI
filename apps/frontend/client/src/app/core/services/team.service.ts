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