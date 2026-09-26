import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';


export interface IntegrationReequest{
    integrationName: string;
    reason: string;
}

@Component({
    selector: 'app-integartion-request-dialog',
    standalone: true,
    imports: [
        FormsModule,
        MatInputModule,
        MatFormFieldModule,
        MatButtonModule,
        MatDialogModule
    ],
    templateUrl: './integration-request-dialog.component.html',
    styleUrl: './integration-request-dialog.component.scss'
})

export class IntegrationRequestDialogComponent{
    private readonly dialogRef= inject(
        MatDialogRef<IntegrationRequestDialogComponent>
    );

    integrationName='';
    reason='';

    submit():void{
        const integrationReequest: IntegrationReequest={
            integrationName: this.integrationName.trim(),
            reason: this.reason.trim()
        };

        this.dialogRef.close(integrationReequest);
    }

    cancel(): void{
        this.dialogRef.close();
    }
}