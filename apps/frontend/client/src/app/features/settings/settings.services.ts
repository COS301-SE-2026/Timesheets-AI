// Author: Cleopatra Kwenda
// Date:2026-08-31
// Purpose: this is for integrating with
// the api, but rn mocked till endpoints are ready
// Related Requirement: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of, forkJoin, map, catchError, throwError } from "rxjs";
import { UserSettings, ChangePasswordRequest, ChangePasswordResponse, MessageResponse, MfaSetupResponse } from "./settings.model";


@Injectable({ providedIn: 'root'})
export class SettingsService{
    private readonly http= inject(HttpClient);
    private readonly apiUrl= 'api/settings';
    private readonly authUrl= 'api/auth';
    private readonly accountUrl= 'api/account';


    private readonly mockSettings: UserSettings={
        security:{
            mfaEnabled:true,
        },
        integrations:[
            {
                id: 'github',
                name: 'GitHub',
                description: 'Sync pull requests, commits and repositories.',
                icon: 'fa-brands fa-github',
                connected: true,
            },
            {
                id: 'jira',
                name: 'Jira',
                description: 'Import issues, track work and link time entries.',
                icon: 'fa-brands fa-jira',
                connected: true,
            },
            {
                id: 'calendar',
                name: 'Calendar',
                description: 'Sync your calendar events and availability.',
                icon: 'fa-brands fa-calendar-days',
                connected: true,
            },
        ],
        notifications:{
            notificationType: 'ALL',
            doNotDisturbEnd: '00:00',
            doNotDisturbStart: '00:00',
            doNotDisturbEnabled: false,
        },
    };

    changePassword(request: ChangePasswordRequest): Observable<ChangePasswordResponse>{
        return this.http.post<ChangePasswordResponse>(
            `${this.authUrl}/change-password`,
            request
        );
    }

    getSettings(mfaEnabled: boolean): Observable<UserSettings>{
        return forkJoin({
            github: this.isConnected('api/integration/github/status'),
            jira: this.isConnected('api/integration/jira/status'),
            calendar: this.isConnected('api/calendar/status'),
        }).pipe(
            map(({ github, jira, calendar})=>{
                const connectionStatus: Record<string, boolean>={
                    github,
                    jira,
                    calendar,
                };
                return{
                    ...this.mockSettings,
                    security: { mfaEnabled},
                    integrations:this.mockSettings.integrations.map((i)=>({
                        ...i,
                        connected: connectionStatus[i.id]?? false,
                    })),
                };
                
            })
        );
    }

    private isConnected(url: string): Observable<boolean>{
        return this.http.get<{ connected: boolean }>(url).pipe(
            map((res)=> res.connected), catchError(()=>of(false))
        )
    }

    getConnectUrl(
        integrationId: string,
        calendarProvider: 'google' | 'microsoft' = 'google'
    ): Observable<string>{
        const urls: Record<string, string>={
            github: 'api/integration/github/connect',
            jira: 'api/integration/jira/connect',
            calendar: 'api/integrations/google/calendar/connect',
        };

        if(integrationId=== 'calendar' && calendarProvider=== 'microsoft'){
            urls['calendar']= 'api/integrations/microsoft/calendar/connect';
        }

        const url=urls[integrationId];
        if(!url){
            return throwError(()=> new Error(`Unknown integration: ${integrationId}`));
        }

        if(integrationId=== 'github'){
            return this.http.get(url, {
                responseType: 'text',
                params:{
                    returnPath: '/settings',
                },
            });
        }
        return this.http.get(url, { responseType: 'text'});
    }

    getMfaSetup(): Observable<MfaSetupResponse>{
        return this.http.get<MfaSetupResponse>(
            `${this.authUrl}/mfa/setup`
        );
    }

    verifyMfa(totpCode: string): Observable<MessageResponse>{
        return this.http.post<MessageResponse>(
            `${this.authUrl}/mfa/verify`,
            { totpCode }
        );
    }

    disableMfa(password: string): Observable<MessageResponse>{
        return this.http.post<MessageResponse>(
            `${this.authUrl}/mfa/disable`,
            { password }
        );
    }

    requestAccountDeletion(reason: string): Observable<MessageResponse>{
        return this.http.post<MessageResponse>(
            `${this.accountUrl}/deletion/request`,
            { reason }
        );
    }
}