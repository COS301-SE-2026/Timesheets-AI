import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import {CreateProjectRequest, ProjectDetailResponse, ProjectResponse, ProjectService } from '../../core/services/project.service';
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
  imports: [CommonModule, FormsModule],
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
    protected actionMessageKind: 'success' | 'error' = 'success';
    protected selectedMember?: TeamMember;
    protected selectedProjectId = '';
    protected assignAsProjectManager = false;
    protected actionInProgress = '';
    protected openActionMenuUserId = '';
    protected assignmentMode: 'add' | 'remove' = 'add';
    protected memberToRemoveFromWorkspace?: TeamMember;
    protected showCreateProjectDialog = false;
    protected newProject: CreateProjectRequest = {name: '', managerIds: []};

    private readonly workspaceMemberIdsByUser = new Map<string, string>();

    protected get isAdmin(): boolean {
      return this.authService.currentUser()?.roles.includes('ROLE_ADMIN') ?? false;
    }
    
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
      this.members = users.map((user) => ({
        ...user,
        workspaceMemberId: user.workspaceMemberId ?? this.workspaceMemberIdsByUser.get(user.userId),
        projectIds: [],
      }));
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
          workspaceMemberId: 
          user.workspaceMemberId ??
          membership?.workspaceMemberId ??
          this.workspaceMemberIdsByUser.get(user.userId),
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
  .map((id) => this.projects.find((project) => project.id === id)?.name)
  .filter((name): name is string => !!name)
}

protected initials(member: TeamMember): string {
  return `${member.firstName.charAt(0)}${member.lastName.charAt(0)}`.toUpperCase();
}

protected openAssignment(member: TeamMember): void {
  this.openActionMenuUserId = '';
  this.selectedMember = member;
  this.selectedProjectId = this.projects.find((project) => !member.projectIds.includes(project.id))?.id ?? '';
  this.assignAsProjectManager = false;
  this.assignmentMode = 'add';
  this.actionMessage = '';
}

protected openProjectRemoval(member: TeamMember) : void {
  this.openActionMenuUserId = '';
  this.selectedMember = member;
  this.selectedProjectId = member.projectIds[0] ?? '';
  this.assignmentMode = 'remove';
  this.actionMessage = '';

}

protected closeAssignment(): void {
  //this.selectedMember = undefined;
  if (!this.actionInProgress) this.selectedMember = undefined;
}

// Actions toggle menu
protected toggleActionMenu(member: TeamMember): void {
  this.openActionMenuUserId = this.openActionMenuUserId === member.userId ? '' : member.userId; 
}

// Remove user from a workspace
protected requestWorkspaceRemoval(member: TeamMember) : void {
  this.openActionMenuUserId = '';
  this.memberToRemoveFromWorkspace = member;
}

// Cancel workspace removal
protected cancelWorkspaceRemoval(): void {
  if (!this.actionInProgress) this.memberToRemoveFromWorkspace = undefined;
}

// Remove fom workspace
protected removeFromWorkspace(): void {
  const member = this.memberToRemoveFromWorkspace;
  if (!member?.workspaceMemberId) return;

  this.startAction(`workspace-remove-${member.userId}`);
  this.teamService.removeFromWorkspace(member.workspaceMemberId)
  .pipe(finalize(() => this.finishAction()))
  .subscribe({
    next: () => {
      this.memberToRemoveFromWorkspace = undefined;
      this.showSuccess(`${member.firstName} has been removed from the workspace.`);
      this.loadTeam();
    },
    error: (error) => this.showError(error.error?.message ?? 'Could not remove this user from the workspace.'),
  });
}

//  Open create project dialog

protected openCreateProject(): void {
  this.newProject = { name: '', managerIds: []};
  this.showCreateProjectDialog = true;
  this.actionMessage = '';
}

// Close create project dialog

projected closeCreateProject(): void {
  if (!this.actionInProgress) this.showCreateProjectDialog = false;
}

// Create project

protected createProject(): void {
  if (!this.newProject.name.trim()) return;
  const request: CreateProjectRequest = {
    ...this.newProject,
    name: this.newProject.name.trim(),
    managerIds: this.isAdmin ? this.newProject.managerIds : [],
  };

  this.startAction('create-project');
  this.projectService.createProject(request)
  .pipe(finalize(() => this.finishAction()))
  .subscribe({
    next: (project) => {
      this.showCreateProjectDialog = false;
      this.showSuccess(`${project.name} has been created.`);
      this.loadTeam();
    },
    error: (error) => this.showError(error.error?.message ?? 'Could not create the project.'),
  });
}


// admin assigns member to workspace
protected addToWorkspace(member: TeamMember): void {
  const actionKey = `workspace-${member.userId}`;
  this.startAction(actionKey);
  this.teamService.addToWorkspace(member.userId, 'DEVELOPER').pipe(finalize(() => this.finishAction())).subscribe({
    next: (workspaceMember) => {
      this.workspaceMemberIdsByUser.set(member.userId, workspaceMember.workspaceMemberId);
      this.showSuccess(`${member.firstName} has been added to the workspace.`);
      this.activeTab = 'members';
      this.loadTeam();
    },
    error: (error) => this.showError(error.error?.message ?? 'Could not add this user to the workspace.'),
  });
}

protected projectName(projectId: string): string {
  return this.projects.find((project) => project.id === projectId)?.name ?? '';
}

// assign user to project
protected assignToProject(): void {
  const member = this.selectedMember;
  if (!member?.workspaceMemberId || !this.selectedProjectId) return;

  this.startAction(`project-${member.userId}`);
  this.teamService.addToProject(this.selectedProjectId, member.workspaceMemberId, this.assignAsProjectManager)
    .pipe(finalize(() => this.finishAction()))
    .subscribe({
      next: () => {
        this.selectedMember = undefined;
        this.showSuccess(`${member.firstName} has been assigned to the project.`);
        this.loadTeam();
      },
      error: (error) => this.showError(error.error?.message ?? 'Could not assign this meber to the project.'),
    });
}

// remove member from project
protected removeFromProject(member: TeamMember, projectId: string): void {
  if (!member.workspaceMemberId) return;
  this.startAction(`remove-${member.userId}-${projectId}`);
  this.teamService.removeFromProject(projectId, member.workspaceMemberId)
  .pipe(finalize(() => this.finishAction()))
  .subscribe({
    next: () => {
      this.showSuccess(`${member.firstName} has been removed from ${this.projectName(projectId)}.`);
      this.loadTeam();
    },
    error: (error) => this.showError(error.error?.message ?? 'Could not remove this member from the project.'),
  });
}

protected isActionInProgress(actionKey: string): boolean {
  return this.actionInProgress === actionKey;
}

protected canManageProjects(member: TeamMember): boolean {
  return Boolean(member.workspaceMemberId) && this.projects.some((project) => !member.projectIds.includes(project.id));
}

protected hasProjectMembershipId(member: TeamMember): boolean {
  return Boolean(member.workspaceMemberId);
}

// Start action function
private startAction(actionKey: string): void {
  this.actionMessage = '';
  this.actionInProgress = actionKey;
} 

private finishAction(): void {
  this.actionInProgress = '';
}

private showSuccess(message: string): void {
  this.actionMessageKind = 'success';
  this.actionMessage = message;
}

private showError(message: string): void {
  this.actionMessageKind = 'error';
  this.actionMessage = message;
}

// filter users
private filterUsers(users: TeamMember[]): TeamMember[] {
  const term = this.searchTerm.trim().toLowerCase();
  if (!term) return users;
  return users.filter((user) => `${user.firstName} ${user.lastName} ${user.email}`.toLowerCase().includes(term));
}

}
