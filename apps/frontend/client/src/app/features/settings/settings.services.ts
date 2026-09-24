// Author: Cleopatra Kwenda
// Date:2026-08-31
// Purpose: this is for integrating with
// the api, but rn mocked till endpoints are ready
// Related Requirement: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of, delay } from "rxjs";
import { UserSettings, IntegrationStatus, NotificationSettings, ChangePasswordRequest, ChangePasswordResponse, MessageResponse, MfaSetupResponse } from "./settings.model";


@Injectable({ providedIn: 'root'})
export class SettingsService{
    private readonly http= inject(HttpClient);
    private readonly apiUrl= 'api/settings';
    private readonly authUrl= 'api/auth';

    private readonly mockSettings: UserSettings={
        security:{
            mfaEnabled:true,
        },
        integrations:[
            {
                id: 'github',
                name: 'Github',
                description: 'Sync pull requests, commits and repositories.',
                icon: 'fa-brands fa-github',
                connected: true,
                enabled: true,
            },
            {
                id: 'jira',
                name: 'Jira',
                description: 'Import issues, track work and link time entries.',
                icon: 'fa-brands fa-jira',
                connected: true,
                enabled: true,
            },
            {
                id: 'google-calendar',
                name: 'Google Calendar',
                description: 'Sync your calendar events and avaiability.',
                icon: 'fa-brands fa-google',
                connected: true,
                enabled: true,
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

    getSettings(): Observable<UserSettings>{
        return of(this.mockSettings).pipe(delay(200));
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

    toggleIntegration(integrationId: string, enabled: boolean): Observable<IntegrationStatus>{
        const integration=this.mockSettings.integrations.find(
            (i)=> i.id=== integrationId
        );

        if(integration) integration.enabled= enabled;

        return of(integration as IntegrationStatus).pipe(delay(150));
    }

    requestIntegration(): Observable<void>{
        return of(void 0).pipe(delay(150));
    }

    updateNotifications(notifications: NotificationSettings): Observable<void>{
        this.mockSettings.notifications= notifications;

        return of(void 0).pipe(delay(150));
    }

    requestAccountDeletion(): Observable<void>{
        return of(void 0).pipe(delay(150));
    }
}