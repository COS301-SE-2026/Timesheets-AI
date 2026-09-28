/**
 * PROJECTS COMPONENT
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-17
 * Purpose: will display all the projects assign to the user.
 * Updated: 2026-07-28, wired to ProjectService instead of the projects.mock
 * fixture. List endpoint renders cards immediately; detail endpoint fills in
 * hoursLogged/progressPercentage/team avatars per card as each call resolves.
 * Purpose: will display all the projects assign to the user.
 * Related Requirement: N/A
 * Responsibilities:
 *  -will display project stats
 *  -will filter project
 *  -will search projects
 */

import { Component, OnInit, inject } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { RouterModule } from '@angular/router';
import { catchError, map} from 'rxjs/operators';
import { Project } from './models/project.model';
import { ProjectStatus } from './enums/project-status.enum';
import { PROJECT_FILTERS } from './constants/project-filters.constant';
import { CommonModule, NgClass } from '@angular/common';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';
import { ProjectService } from '../../core/services/project.service';
import { AuthService } from '../../core/services/auth.service';
import {
  mapToProjectCard,
  applyProjectDetail,
  extractMyHoursFromDetail,
  formatHoursMinutes
} from './utils/project-mapper';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ProgressBarComponent,
    NgClass
  ],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
})
  
export class ProjectsComponent implements OnInit {
  private readonly projectService = inject(ProjectService);
  private readonly authService = inject(AuthService);

  protected projects: Project[] = [];
  protected filteredProjects: Project[] = [];
  protected readonly filters = PROJECT_FILTERS;
  protected selectedFilter = 'All';
  protected searchTerm = '';

  protected loading = true;
  protected error = false;

  //Hours the logged in user has personally logged, summed across
  //all their projects, not the team total. 
  protected myTotalHoursLoading = true;
  protected myTotalHoursLabel = '0h 0m'

  ngOnInit(): void {
      this.loading = true;
      this.error = false;

      this.projectService.getProjects().subscribe({
        next: (list) => {
          this.projects = list.map(mapToProjectCard);
          this.filteredProjects = [...this.projects];
          this.loading = false;
          this.loadDetailsForAllCards();
        },
        error: () => {
          this.loading = false;
          this.error = true;
        },
      });
  }
  private loadDetailsForAllCards(): void{
    if(this.projects.length === 0){
      this.myTotalHoursLoading = false;
      this.myTotalHoursLabel = formatHoursMinutes(0);
      return;
    }
    const currentUserEmail = this.authService.currentUser()?.email ?? '';

    const detailCalls = this.projects.map((card) =>
      this.projectService.getProjectDetail(card.id).pipe(
        map((detail) =>{
          applyProjectDetail(card, detail);
          return extractMyHoursFromDetail(detail, currentUserEmail);
        }),
        catchError(() => {
          card.detailLoaded = true;
          card.detailError = true;
          return of(0); //so one failed card shouldn't sink the whole top stat
        }),
      ),
    );
    forkJoin(detailCalls).subscribe((myHoursPerProject) => {
      const total = myHoursPerProject.reduce((sum, h) => sum+h, 0);
      this.myTotalHoursLabel = formatHoursMinutes(total);
      this.myTotalHoursLoading = false;
    });
  }

  protected get totalProjects(): number {
    return this.projects.length;
  }

  protected get summaryCards() {
    return [
      { label: 'Total projects', description: 'All your assigned projects', value: this.totalProjects, icon: 'fa-folder-open', color: 'blue'},
      { label: 'Active projects', description: 'Projects you contribute to', value: this.activeProjects, icon: 'fa-folder', color: 'green'},
      { label: 'Completed projects', description: 'Projects delivered', value: this.completedProjects, icon: 'fa-circle-check', color: 'orange'},
      { label: 'Your hours', description: 'Time across projects', value: this.myTotalHoursLoading ? '...' : this.myTotalHoursLabel, icon: 'fa-clock', color: 'purple'},
    ]
  }

  protected get activeProjects(): number {
    return this.projects.filter(
      (project) => project.status === ProjectStatus.ACTIVE,
    ).length;
  }

  protected get completedProjects(): number {
    return this.projects.filter(
      (project) => project.status === ProjectStatus.COMPLETED,
    ).length;
  }

  protected get onHoldProjects(): number {
    return this.projects.filter(
      (project) => project.status === ProjectStatus.ON_HOLD,
    ).length;
  }

  protected get totalHours(): number {
    return this.projects.reduce(
      (totalHours, project) => totalHours + (project.hoursLogged ?? 0),
      0,
    );
  }

  protected get visibleFilters(): string[] {
    const user = this.authService.currentUser();
    const isManager = user?.roles.some(role => 
    ['MANAGER', 'ROLE_MANAGER', 'ADMIN', 'ROLE_ADMIN'].includes(role)) ?? false;
    return isManager ? this.filters : this.filters.filter(f => f !== 'My projects');
  }

  protected filterProjects(selectedFilter: string): void {
    if (selectedFilter === 'My projects' && !this.canSeeMyProjects) {
      return;
    }
    this.selectedFilter = selectedFilter;

    this.filteredProjects = this.projects.filter(
      (project) => (selectedFilter === 'All' ||
        (selectedFilter === 'My projects' ? project.role !== null : project.status === selectedFilter )) &&
        project.name.toLowerCase().includes(this.searchTerm.toLowerCase()),
    );
  }

  

  protected searchProjects(searchValue: string): void {
    this.searchTerm = searchValue;
    this.filterProjects(this.selectedFilter);
  }

  protected getProjectInitials(name:string):string{
    if(!name) return '';
    return name
        .split(' ')
        .map(word=> word[0])
        .join('')
        .substring(0, 2)
        .toUpperCase();
    }

}
