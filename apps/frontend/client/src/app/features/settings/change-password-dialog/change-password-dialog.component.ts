import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule} from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field'
import { MatInputModule} from '@angular/material/input';
import { SettingsService } from '../settings.services';
;

@Component({
  selector: 'app-change-password-dialog',
  imports: [ CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './change-password-dialog.component.html',
  styleUrl: './change-password-dialog.component.scss'
})

export class ChangePasswordDialogComponent {
  private settingsService= inject(this.settingsService);
  private dialogRef= inject(MatDialogRef<ChangePasswordDialogComponent>);

  currentPassword='';
  newPassword='';
  confirmPassword='';
  errorMessage= '';
  isSubmitting= false;

  get passwordMismatch():boolean{
    return this.confirmPassword.length>0 && this.newPassword !== this.confirmPassword;
  }

  submit(): void{
    this.errorMessage= '';

    if(this.passwordMismatch){
      this.errorMessage='New password and confirmation do not match.'
      return;
    }

    if(this.newPassword.length< 8){
      this.errorMessage='New password must ne at least 8 characters.'
      return;
    }

    this.isSubmitting=true;

    this.settingsService.changePassword(
      {
        currentPassword: this.currentPassword,
        newPassword: this.newPassword,
        confirmPassword: this.confirmPassword,
      }
    ).subscribe(
      {
        next: ()=>{
          this.isSubmitting= false;
          this.errorMessage= 'Something went wrong. Please try again.'
        }
      }
    );
  }
  cancel():void{
    this.dialogRef.close(false);
  }

}
