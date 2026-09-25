import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef } from '@angular/material/dialog';

interface AvaiableIntegrations{
    id: string;
    name: string;
    description: string;
    icon: string;
}

@Component({
    selector: 'app-integartion-browser-dialog',
    standalone: true,
    imports: [
        CommonModule
    ],
    templateUrl: './integartion-browser-dialog.component.html',
    styleUrl: './integartion-browser-dialog.component.scss'
})

export class IntegrationBrowserDialogComponent{
    private readonly dialogRef= inject(
        MatDialogRef<IntegrationBrowserDialogComponent>
    );

    integrations: AvaiableIntegrations[]= [
        {
            id: 'slack',
            name: 'Slack',
            description: 'Connect your team conversations and notifications.',
            icon: 'fa-brands fa-slack'
        },
        {
            id: 'microsoft-teams',
            name: 'Microsoft Teams',
            description: 'Connect meetings, messages and team activity.',
            icon: 'fa-brands fa-microsoft'
        },
        {
            id: 'notion',
            name: 'Notion',
            description: 'Connect your workspace pages and project information.',
            icon: 'fa-solid fa-book'
        },
        {
            id: 'trello',
            name: 'Trello',
            description: 'Connect boards, cards and project work.',
            icon: 'fa-brands fa-trello'
        }
    ];

    addIntegrations(integartion: AvaiableIntegrations): void{
        this.dialogRef.close(integartion);
    }

    cancel(): void{
        this.dialogRef.close();
    }
}