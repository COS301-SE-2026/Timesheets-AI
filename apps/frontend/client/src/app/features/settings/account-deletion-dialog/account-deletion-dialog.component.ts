import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { FormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { error } from 'node:console';

@Component({
    selector: 'app-account-deletion-dialog',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatInputModule,
        MatFormFieldModule,
        MatButtonModule,
        MatDialogModule
    ],
    templateUrl: './account-deletion-dialog.html',
    styleUrl: './account-deletion-dialog.scss'
})

export class AccountDeletionDialogComponent{
    private readonly dialogRef= inject(
        MatDialogRef<AccountDeletionDialogComponent>
    );

    isSubmitting=false;
    reason='';
    errorMessage='';

    submit():void{
        const trimmedReason= this.reason.trim()

        if(!trimmedReason){
            this.errorMessage= 'Please provide a reason for deleting your account.';
            return;
        }

        this.isSubmitting= true;
        this.errorMessage= '';

        this.settingsService.requestAccountDeletion(trimmedReason).subscribe({
            next:()=>{
                this.dialogRef.close(true);
            },
            error:(error)=> {
                this.isSubmitting= false;
                this.errorMessage= error?.error?.message??'Unable to submit your account deletion request. Please try again.'
            }
        });
    }

    cancel(): void{
        this.dialogRef.close(false);
    }
}