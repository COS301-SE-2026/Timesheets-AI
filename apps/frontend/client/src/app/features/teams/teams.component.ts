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
    protected searchTerm = '';
    protected loading = true;
    protected errorMessage = '';
    protected actionMessage = '';
    protected selectedMember?: TeamMember;
    protected selectedProjectId = '';
    protected assignAsProjectManager = false;

    protected readonly isAdmin = this.authService.currentUser()?.roles.includes('ROLE_ADMIN') ?? false;
  
    ngOnInit(): void {
      this.loadTeam();
    }

    protected loadTeam(): void {
      this.loading = true;
      this.errorMessage = '';

      forkJoin({
        users: this.teamService.getAvailableUsers(),
        projects: this.projectService.getProjects(),
      }).subscribe({
        next: ({ users, projects }) => this.loadProjectMemberships(users, projects),
        error: () => {
          this.loading = false;
          this.errorMessage = 'We could not load the team. Please try again.';
        },
      });
    }

    // load project details and workspace member to the projects they belong to
    private loadProjectMemberships(users: AvailableTeamUser[], projects: ProjectResponse[]): void {
    this.projects = projects.map(({ id, name }) => ({ id, name }));
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
      /* Project details supply user IDs alongside membership IDs. Match by email to bridge
         the current Team API, whose available-user response lacks workspaceMemberId. */
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

protected get workspaceMembers(): TeamMember[] {
  return this.filterUsers(this.members.filter((member) => member.isInWorkspace));
}

protected get waitingUsers(): TeamMember[] {
  return this.filterUsers(this.members.filter((member) => !member.isInWorkspace));
}

protected get visibleUsers(): TeamMember[] {
  return this.activeTab === 'members' ? this.workspaceMembers : this.waitingUsers;
}

protected projectNames(member: TeamMember): string[] {
  return member.projectIds
  .map((id) => this.projects.find((projects) => project.id === id)?.name)
  .filter((name): name is string => !!name)
}

protected initials(member: TeamMember): string {
  return `${member.firstName.charAt(0)}${member.lastName.charAt(0)}`.toUpperCase();
}

protected openAssignment(member: TeamMember): void {
  this.selectedMember = member;
  this.selectedProjectId = this.projects.find((project) => !member.projectIds.includes(project.id))?.id ?? '';
  this.assignAsProjectManager = false;
  this.actionMessage = '';
}

protected closeAssignment(): void {
  this.selectedMember = undefined;
}

// admin assigns member to workspace
protected addToWorkspace(member: TeamMember): void {
  this.actionMessage = '';
  this.teamService.addToWorkspace(member.userId, 'DEVELOPER').subscribe({
    next: () => {
      this.actionMessage = `${member.firstName} has been added to the workspace.`;
      this.loadTeam();
    },
    error: (error) => this.actionMessage = error.error?.message ?? 'Could not add this to the workspace.'
  });
}

// assign user to project
protected assignToProject(): void {
  const member = this.selectedMember;
  if (!member?.workspaceMemberId || !this.selectedProjectId) return;

  this.teamService.addToProject(this.selectedProjectId, member.workspaceMemberId, this.assignAsProjectManager).subscribe({
    next: () => {
      this.actionMessage = `${member.firstName} has been assigned to the project.`;
      this.closeAssignment();
      this.loadTeam();
    },
    error: (error) => this.actionMessage = error.error?.message ?? 'Could not assign member to the project.',
  })
}

// remove member from project
protected removeFromProject(member: TeamMember, projectId: string): void {
  if (!member.workspaceMemberId) return;
  this.actionMessage = '';
  this.teamService.removeFromProject(projectId, member.workspaceMemberId).subscribe({
    next: () => {
      this.actionMessage = `${member.firstName} has removed from the project.`;
      this.loadTeam();
    },
    error: (error) => this.actionMessage = error.error?.message ?? 'Could not remove this member from the project.',
  });
}

// filter users
private filterUsers(users: TeamMember[]): TeamMember[] {
  const term = this.searchTerm.trim().toLowerCase();
  if (!term) return users;
  return users.filter((user) => `${user.firstName} ${user.lastName} ${user.email}`.toLowerCase().includes(term));
}

}
