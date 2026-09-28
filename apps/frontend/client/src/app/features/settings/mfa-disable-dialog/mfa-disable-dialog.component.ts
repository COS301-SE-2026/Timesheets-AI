// // Author: Cleopatra Kwenda
// Date:2026-09-24
// Purpose: this is for the MFA disable confirmation
// and the password pop-up
// Related Requirement: N/A

import { Component, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { SettingsService } from "../settings.services";

@Component({
    selector: 'app-mfa-disable-dialog',
    standalone: true,
    imports:[
        CommonModule,
        FormsModule,
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule
    ],
    templateUrl:'./mfa-disable-dialog.component.html',
    styleUrl: './mfa-disable-dialog.component.scss'
})

export class MfaDisableDialogComponent {
    private readonly settingsService= inject(SettingsService);
    private readonly dialogRef= inject(MatDialogRef<MfaDisableDialogComponent>);

    password= '';
    errorMessage= '';
    isSubmitting= false;
    isSuccess= false;


    disable(): void{
        this.errorMessage='';

        if(!this.password.trim()){
            this.errorMessage= 'Please enter your password.';
            return;
        }

        this.isSubmitting= true;
        this.settingsService.disableMfa(this.password).subscribe({
            next:()=> {
                this.isSubmitting= false;
                this.isSuccess= true;
            },
            error:(error)=> {
                this.isSubmitting= false;

                if(error.status === 400 || error.status=== 401){
                    this.errorMessage= error.error?.message || 'The password is incorrect.';
                }else{
                    this.errorMessage= 'Something went wrong. Please try again.';
                }
            }
        });
    }

    done(): void{
        this.dialogRef.close(true);
    }

    cancel(): void{
        this.dialogRef.close(false);
    }

}