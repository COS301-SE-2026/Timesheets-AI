/**
 * PROJECTS COMPONENT
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-17
 * Purpose: will display all the projects assign to the user.
 * Related Requirement: N/A
 * Responsibilities:
 *  -will display project stats
 *  -will filter project
 *  -will search projects
 */

import { Component } from '@angular/core';
import { Project } from './models/project.model';
import { ProjectStatus } from './enums/project-status.enum';
import { PROJECT_FILTERS } from './constants/project-filters.constant';
import { PROJECTS } from './mock/projects.mock';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { StatsCardComponent } from '../../shared/components/stats-card/stats-card.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';
import { StatusChipComponent } from '../../shared/components/status-chip/status-chip.component';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    HeaderComponent,
    StatsCardComponent,
    ProgressBarComponent,
    StatusChipComponent,
  ],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss',
})
export class ProjectsComponent {
  protected readonly projects: Project[] = PROJECTS;
  protected filteredProjects: Project[] = [...PROJECTS];
  protected readonly filters = PROJECT_FILTERS;
  protected selectedFilter = 'All';
  protected searchTerm = '';

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

  protected get totalHours(): number {
    return this.projects.reduce(
      (totalHours, project) => totalHours + project.hoursLogged,
      0,
    );
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
}
