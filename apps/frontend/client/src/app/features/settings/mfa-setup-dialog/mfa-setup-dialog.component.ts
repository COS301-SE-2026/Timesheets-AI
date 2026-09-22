// // Author: Cleopatra Kwenda
// Date:2026-08-31
// Purpose: this is for the MFA qr code and otp
//  code pop-up
// Related Requirement: N/A

import { Component, OnInit, inject } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatInputModule } from "@angular/material/input";
import { SettingsService } from "../settings.services";
import { error } from "node:console";

@Component({
    selector: 'app-mfa-setup-dialog',
    standalone: true,
    imports:[
        CommonModule,
        FormsModule,
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule
    ],
    templateUrl:'./mfa-setup-dialog.component.html',
    styleUrl: './mfa-setup-dialog.component.scss'
})

export class MfaSetupDialogComponent implements OnInit{
    private readonly settingsService= inject(SettingsService);
    private readonly dialogRef= inject(MatDialogRef<MfaSetupDialogComponent>);

    secretKey= '';
    qrCodeUrl= '';
    totpCode= '';
    errorMessage= '';

    isLoading= true;
    isSubmitting= false;
    isSuccess= false;

    ngOnInit(): void {
        this.settingsService.getMfaSetup(). subscribe({
            next:( response)=>{
                this.secretKey= response.secretKey;
                this.qrCodeUrl= response.qrCodeUrl;
                this.isLoading= false;
            },
            error: (error)=>{
                this.isLoading= false;

                this.errorMessage=error.error?.message || 'Unable to start MFA setup. Please try again.'
            }
        });
    }

    verify(): void{
        this.errorMessage='';

        const otp= this.totpCode.trim();
        if(!/^\d{6}$/.test(otp)){
            this.errorMessage= 'Please enter the 6 digit code from your authenticator app.';
            return;
        }

        this.isSubmitting= true;
        this.settingsService.verifyMfa(otp).subscribe({
            next:()=> {
                this.isSubmitting= false;
                this.isSuccess= true;
            },
            error:(error)=> {
                this.isSubmitting= false;

                if(error.status === 400){
                    this.errorMessage= error.error?.message || 'Invalid authentication code.';
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