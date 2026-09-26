import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSlideToggleChange, MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule} from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { SettingsService } from './settings.services';
import { UserSettings, UserRole, IntegrationStatus } from './settings.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ChangePasswordDialogComponent } from './change-password-dialog/change-password-dialog.component';
import { MfaSetupDialogComponent } from './mfa-setup-dialog/mfa-setup-dialog.component';
import { MfaDisableDialogComponent } from './mfa-disable-dialog/mfa-disable-dialog.component';
import { AuthService } from '../../core/services/auth.service';
import { IntegrationBrowserDialogComponent } from './integration-browser-dialog/integration-browser-dialog.component';
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MatSlideToggleModule, 
    MatSelectModule, 
    MatFormFieldModule,
    MatDialogModule
  ],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})

export class SettingsComponent implements OnInit{
  private readonly settingsService= inject(SettingsService);
  private readonly authService= inject( AuthService);
  private readonly dialog=inject(MatDialog);

  settings= signal<UserSettings | null>(null);
  role= signal<UserRole>('DEVELOPER');
  isLoading= signal<boolean>(true);

  // ROLE BASE PERMISSIONS
  isAdmin= computed(
    ()=> this.role()=== 'ADMIN'
  );
  isManager= computed(
    ()=> this.role()=== 'MANAGER'
  );
  isDeveloper= computed(
    ()=> this.role()=== 'DEVELOPER'
  );

  // DEVS CAN ONLY SEE INTEGRATIONS
  canToggleIntegrations= computed(
    ()=> this.isAdmin() || this.isManager()
  );

  // ONLY ADMIN CAN ADD NEW INTEGRATIONS + MANAGER REQUEST NEW ONES
  canAddIntegrations= computed(
    ()=> this.isAdmin()
  );

  canRequestIntegrations= computed(
    ()=> this.isManager()
  );

  ngOnInit(): void{
    const user= this.authService.currentUser();

    if(!user){
      this.isLoading.set(false);
      return;
    }

    let role: UserRole;

    if(user.roles.includes('ROLE_ADMIN')){
      role='ADMIN';
    }else if (user.roles.includes('ROLE_MANAGER')){
      role= 'MANAGER';
    }else{
      role= 'DEVELOPER';
    }

    this.role.set(role);

    this.settingsService.getSettings(user.mfaEnabled).subscribe((settings)=> {
      this.settings.set(settings);
      this.isLoading.set(false)
    });
  }

  browseIntegrations(): void{
    if(!this.canAddIntegrations()){
      return;
    }

    const dialogRef= this.dialog.open(
      IntegrationBrowserDialogComponent,{
        width: '700px',
        maxWidth: '120vw',
        disableClose: true
      }
    );

    dialogRef.afterClosed().subscribe((integration)=> {
      if(!integration){
        return;
      }

      console.log('Integration selected:', integration);
    });
  }

  changePassword():void{
    // i need the password change flow that our app uses
    const dialogRef= this.dialog.open(
      ChangePasswordDialogComponent,{
        width:'450px',
        maxWidth: '95vw',
        disableClose: true,
        panelClass: 'integration-browser-dialog'
      }
    );

    dialogRef.afterClosed().subscribe(
      (changed)=>{
        if (changed){
          console.log('Password changed successfully.')
        }
      }
    );
  }

  toggleMfa(event: MatSlideToggleChange):void{
    const currentlyEnabled= !event.checked;
    event.source.checked= currentlyEnabled;

    if(currentlyEnabled){
      this.disableMfa();
    }else{
      this.enableMfa();
    }
  }

  private enableMfa(): void{
    const dialogRef= this.dialog.open(
      MfaSetupDialogComponent,
      {
        width: '480px',
        maxWidth: '95vw',
        disableClose: true
      }
    );

    dialogRef.afterClosed().subscribe((enabled)=> {
      if(!enabled){
        return;
      }

      this.authService.updateMfaStatus(true);

      this.settings.update((s)=> (
        s? {...s, security: {
          ...s.security, mfaEnabled:true
        }}:s
      ));
    });
  }

  private disableMfa(): void{    
    const dialogRef= this.dialog.open(
      MfaDisableDialogComponent,
      {
        width: '480px',
        maxWidth: '95vw',
        disableClose: true
      }
    );

    dialogRef.afterClosed().subscribe((disabled)=> {
      if(!disabled){
        return;
      }

      this.authService.updateMfaStatus(false);

      this.settings.update((s)=> (
        s? {...s, security: {
          ...s.security, mfaEnabled:false
        }}:s
      ));
    });
  }

  toggleIntegration(integration: IntegrationStatus, enabled:boolean):void{
    if(!this.canToggleIntegrations()){
      return;
    }

    this.settingsService.toggleIntegration(integration.id, enabled).subscribe(
      ()=>{
        this.settings.update(
          (s)=>{
            if(!s) return s;

            const integrations= s.integrations.map(
              (i)=> i.id === integration.id? {...i, enabled}: i
            );
            return { ...s, integrations};
          });
      });
  }

  requestIntegration():void{
    this.settingsService.requestIntegration().subscribe(
      ()=>{
        // NOTIFICATION CONFIRMATION HOWEVER THATS DONE
      });
  }

  requestAccountDeletion():void{
    this.settingsService.requestAccountDeletion().subscribe(
      ()=>{
        // CONFIRMATION POPUP MAYBE
      }
    );
  }
}
