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
import { catchError, map} from 'rxjs/operators';
import { Project } from './models/project.model';
import { ProjectStatus } from './enums/project-status.enum';
import { PROJECT_FILTERS } from './constants/project-filters.constant';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { StatsCardComponent } from '../../shared/components/stats-card/stats-card.component';
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
    HeaderComponent,
    StatsCardComponent,
    ProgressBarComponent,
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
  protected myTotalHoursLabel: string = '0h 0m'

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

  protected filterProjects(selectedFilter: string): void {
    this.selectedFilter = selectedFilter;

    if (selectedFilter === 'All') {
      this.filteredProjects = [...this.projects];
      return;
    }

    this.filteredProjects = this.projects.filter(
      (project) => project.status === selectedFilter,
    );
  }

  protected searchProjects(searchValue: string): void {
    this.searchTerm = searchValue;
    this.filteredProjects = this.projects.filter((project) =>
      project.name.toLowerCase().includes(searchValue.toLowerCase()),
    );
  }

  protected filterByProjects(selectedFilter:string): void{
    this.selectedFilter = selectedFilter;
    if(selectedFilter === 'ALL'){
      this.filteredProjects = [...this.projects];
      return;
    }
    this.filteredProjects = this.projects.filter(
      (project) => project.status === this.selectedFilter,
    );
  }
  protected seachProject(searchValue: string): void{
    this.searchTerm = searchValue;
    this.filteredProjects = this.projects.filter((project) =>
      project.name.toLowerCase().includes(searchValue.toLowerCase()),
    );
  }
}
