// call the /api/suggestions endpoints

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SuggestedWorkSession } from './models/suggested-work-session';

@Injectable({
    providedIn: 'root'
})
export class SuggestionService {
    private readonly apiUrl = '/api/suggestions';

    constructor(private http: HttpClient){}

    getSuggestions(workspaceMemberId: string): Observable<SuggestedWorkSession[]>{
        return this.http.get<SuggestedWorkSession[]>(`${this.apiUrl}/workspace-member/${workspaceMemberId}`);
    }

    getSuggestion(suggestionId: string): Observable<SuggestedWorkSession>{
        return this.http.get<SuggestedWorkSession>(`${this.apiUrl}/${suggestionId}`);
    }

    approve(suggestionId: string): Observable<SuggestedWorkSession>{
        return this.http.post<SuggestedWorkSession>('${this.apiUrl}/${suggestionId}/approve', {});
    }

    reject(suggestionId: string): Observable<SuggestedWorkSession>{
        return this.http.post<SuggestedWorkSession>('${this.apiUrl}/${suggestionId}/reject', {});
    }
}