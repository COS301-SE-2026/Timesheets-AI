import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { AuthService } from '../../core/services/auth.service';
import { ProjectDetailResponse, ProjectResponse, ProjectService } from '../../core/services/project.service';
import { AvailableTeamUser, TeamService } from '../../core/services/team.service'; 

interface TeamProject {
  id: string;
  name: string;
}

interface TeamMember extends AvailableTeamUser {
  workspaceMemberId?: string;
  projectIds: string[];
}

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent],
  templateUrl: './teams.component.html',
  styleUrl: './teams.component.scss'
})
export class TeamsComponent implements OnInit {
  private readonly teamService = inject(TeamService);
  private readonly projectService = inject(ProjectService);
  private readonly authService = inject(AuthService);

    protected members: TeamMember[] = [];
    protected projects: TeamProject[] = [];
    protected activeTab: 'members' | 'waiting' = 'members';
    protected searchTeam = '';
    protected loading = true;
    protected errorMessage = '';
    protected actionMessage = '';
    protected selectedMember?: TeamMember;
    protected selectedProjectId = '';
    protected assignAsProjectManager = false;

    protected readonly isAdmin = this.authService.currentUser()?.roles,include('ROLE_ADMIN') ?? false;
  
    ngOnInit(): void {
      this.loadTeam();
    }
    

}
