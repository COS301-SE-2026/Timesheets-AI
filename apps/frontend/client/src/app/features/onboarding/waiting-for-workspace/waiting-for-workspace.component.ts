/**
 * Author: Nyasha
 * Date: 2026/08/29
 * Purpose: Loads a page shown to users that are not part of a workspace.
 */
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { RouterModule, Router } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { DatePipe } from '@angular/common';
import { HourglassComponent } from '../../../shared/components/hourglass/hourglass.component';
import { AuthService, AuthUser } from '../../../core/services/auth.service';

@Component({
  selector: 'app-waiting-for-workspace',
  standalone: true,
  imports: [RouterModule, HourglassComponent,DatePipe],
  templateUrl: './waiting-for-workspace.component.html',  
  styleUrls: ['./waiting-for-workspace.component.scss']
})

export class WaitingForWorkspaceComponent implements OnInit, OnDestroy {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private pollSubscription?: Subscription;
  
  protected readonly brandLogo = '/assets/momently.png';

  //this will be checking if a workspace is currently in progress
  protected checking = false;
  protected lastChecked: Date | null = null;
  protected checkCount = 0;

  /*
  - I chose 30 mins such that the users do not wait too long after the admin assigns them
  - However not enough such that teh DB gets overwhelmed
  */
  private readonly POLL_INTERVAL = 30 * 60 * 1000; //to have the check be every 30 minutes
  private readonly INITIAL_DELAY = 3000; // 3 seconds

  ngOnInit(): void {
    // Initial check after 3 seconds
    setTimeout(() => this.checkWorkspace(), this.INITIAL_DELAY);
    
    //poll every 30 minutes
    this.pollSubscription = interval(this.POLL_INTERVAL).subscribe(() => {
      this.checkWorkspace();
    });
  }

  //this should make sure that the polling does not continue after navigation
  ngOnDestroy(): void {
    // Clean up subscription to prevent memory leaks
    this.pollSubscription?.unsubscribe();
  }

  checkWorkspace(): void {
    //should prevent multiple checks incase the button is spammed and pressed multiple times
    if (this.checking) return;
    
    this.checking = true;
    this.checkCount++; //for transparency so that a user can know how many times something was checked
    
    this.authService.refreshUser().subscribe({
      next: () => {
        this.checking = false;
        this.lastChecked = new Date();
        
        const user = this.authService.currentUser();
        const hasWorkspace = this.userHasWorkspace(user);
        
        if (hasWorkspace) {
          //once they get a workspace, the polling can stop, and they are added to a workspace
          this.pollSubscription?.unsubscribe();
          this.router.navigate(['/log-time']);
        }
      },
      error: () => {
        this.checking = false;
        this.lastChecked = new Date();
        //when there is an error the polling should not stop, it should still continue cause it could be a network issue and be temporary
      }
    });
  }

  private userHasWorkspace(user: AuthUser | null): boolean {
  return user?.roles?.some((role: string) => role === 'ROLE_DEVELOPER' || role === 'ROLE_MANAGER' || role === 'ROLE_ADMIN') ?? false;
}

  //when someone logs out the polling should stop
  logout(): void {
    this.pollSubscription?.unsubscribe();
    this.authService.logout();
  }
}