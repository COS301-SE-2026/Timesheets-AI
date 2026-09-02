import { Component, OnInit, Inject, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule} from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { SettingsService } from './settings.services';
import { CurrentUserService } from './current-user.services';
import { UserSettings, UserRole, IntegrationStatus, NotificationType } from './settings.model';
@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, MatSlideToggleModule, MatSelectModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss'
})

export class SettingsComponent {
  private settingsService= inject(SettingsService);
  private currentUserService= inject( CurrentUserService);
  
}
