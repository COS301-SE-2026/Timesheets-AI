import { Component, OnInit, Inject, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormField, MatSelectModule} from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { SettingsService } from './settings.services';
import { CurrentUserService } from './current-user.services';
import { UserSettings, UserRole, IntegrationStatus, NotificationType } from './settings.model';
import { MatFormFieldModule } from '@angular/material/form-field';
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSlideToggleModule, MatSelectModule, MatFormFieldModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})

export class SettingsComponent {
  private settingsService= inject(SettingsService);
  private currentUserService= inject( CurrentUserService);

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
    this.currentUserService.getCurrentUser().subscribe(
      (user)=>{
        this.role.set(user.role);
      }
    );

    this.settingsService.getSettings().subscribe(
      (settings)=>{
        this.settings.set(settings);
        this.isLoading.set(false);
      }
    );
  }

  changePassword():void{
    // i need the password change flow that our app uses
  }

  toggleMfa(enabled:boolean):void{
    this.settingsService.toggleMfa(enabled).subscribe(
      ()=>{
        this.settings.update(
          (s)=>(
            s? {...s, security:{
              ...s.security, mfaEnabled:enabled
            }}: s
          )
        );
      }
    );
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

  setNotificationType(notificationType: NotificationType):void{
    const current= this.settings();

    if(!current) return;

    const notifications={
      ...current.notifications, notificationType
    };

    this.settingsService.updateNotifications(notifications).subscribe(
      ()=>{
        this.settings.update(
          (s)=>(
            s? {...s, notifications}: s
          )
        );
      }
    );
  }
  requestAccountDeletion():void{
    this.settingsService.requestAccountDeletion().subscribe(
      ()=>{
        // CONFIRMATION POPUP MAYBE
      }
    );
  }
}
