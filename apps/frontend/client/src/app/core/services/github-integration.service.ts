/*
This file handles wrapping the github connect/sync endpoints from Github Integration controller
Author: Zamokuhle Zwane
Date: 03/09/2026
*/

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class GithubIntegrationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/integrations/github`;

  connect(): Observable<string> {
    return this.http.get(`${this.baseUrl}/connect`, {responseType: 'text'});
  }

  sync(): Observable<number>{
    return this.http.post<{number: number}>(`${this.baseUrl}/sync`, {}).pipe(
      map(res => res.number)
    );
  }
}