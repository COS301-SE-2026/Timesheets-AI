/*
Talks to the project community endpoints (messages and reactions).

Author: Zamokuhle Zwane
Date: 28/09/2026
*/

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type ReactionType = 'LIKE' | 'DISLIKE' | 'CHECK' | 'CROSS' | 'IMPORTANT';

export interface ReactionSummary {
  type: ReactionType;
  count: number;
  reactedByMe: boolean;
}

export interface ProjectMessage {
  id: string;
  projectId: string;
  authorWorkspaceMemberId: string;
  authorName: string;
  content: string;
  createdAt: string;
  updatedAt: string;
  mine: boolean;
  reactions: ReactionSummary[];
}

export interface ProjectMessageLocation {
  projectId: string;
  messageId: string;
}

@Injectable({ providedIn: 'root' })
export class ProjectCommunityService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/projects';

  getMessages(projectId: string): Observable<ProjectMessage[]> {
    return this.http.get<ProjectMessage[]>(`${this.baseUrl}/${projectId}/messages`);
  }

  sendMessage(projectId: string, content: string): Observable<ProjectMessage> {
    return this.http.post<ProjectMessage>(`${this.baseUrl}/${projectId}/messages`, { content });
  }

  deleteMessage(projectId: string, messageId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}/messages/${messageId}`);
  }

  // adds or removes my reaction; the response is the updated message
  toggleReaction(
    projectId: string,
    messageId: string,
    reactionType: ReactionType,
  ): Observable<ProjectMessage> {
    return this.http.post<ProjectMessage>(
      `${this.baseUrl}/${projectId}/messages/${messageId}/reactions`,
      { reactionType },
    );
  }

  // used by notifications to find the project a message belongs to
  getMessageLocation(messageId: string): Observable<ProjectMessageLocation> {
    return this.http.get<ProjectMessageLocation>(`/api/project-messages/${messageId}/location`);
  }
}