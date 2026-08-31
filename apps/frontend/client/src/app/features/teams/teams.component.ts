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

    protected loadTeam(): void {
      this.loading = true;
      this.errorMessage = '';

      forkJoin({
        users: this.teamService.getAvailableUsers(),
        projects: this.projectService.getProjects(),
      }).subcricbe({
        next: ({ users, projects }) => this.loadProjectMemberships(users, projects),
        error: () => {
          this.loading = false;
          this.errorMessage = 'We could not load the team. Please try again.';
        },
      });
    }

    // load project details and workspace member to the projects they belong to
    private loadProjectMemberships(users: AvailableTeamUser[], projects: ProjectResponse[]) : void {
      this.projects = projects.map(({ id, name }) => ({id, name }));
      if (!projects.length) {
        this.members = users.map((user) => ({ ...user, projectIds: [] }));
        this.loading = false;
        return;
      }

      forkJoin(
        projects.map((project) =>
          this.projectService.getProjectDetail(project.id).pipe(
            catchError(() => of(null)),
          ),
        ),
      ).subscribe((details) => {
        const membershipByEmail = new Map<string, { workspaceMemberId: string; projectIds: string[] }>();
        details.filter((detail): detail is ProjectDetailResponse => detail !== null).forEach((detail) => {
          detail.members.forEach((member) => {
          const match = membershipByEmail.get(member.email) ?? {
            workspaceMemberId: member.workspaceMemberId,
            projectIds: [],
          };
          if (!match.projectIds.includes(detail.id)) match.projectIds.push(detail.id);
          membershipByEmail.set(member.email, match);
        });
      });

      this.members = users.map((user) => {
        const membership = membershipByEmail.get(user.email);
        return {
          ...user,
          workspaceMemberId: membership?.workspaceMemberId,
          projectIds: membership?.projectIds ?? [],
        };
      });
      this.loading = false;
    });
}

protected get workspaceMember(): TeamMember[] {
  return this.filterUsers(this.members.filter((member) => member.isInWorkspace));
}

protected waitingUsers(): TeamMember[] {
  return this.filterUsers(this.members.filter((member) => !member.isInWorkspace));
}

protected get visibleUsers(): TeamMember[] {
  return this.activeTab === 'members' ? this.workspaceMembers : this.waitingUsers;
}

protected initials(member: TeamMember): string {
  return `${member.firstName.charAt(0)}${member.lastName.charAt(0)}`.toUpperCase();
}
}
