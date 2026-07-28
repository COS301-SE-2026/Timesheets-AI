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
import { CommonModule, NgClass } from '@angular/common';
import { HeaderComponent } from '../../shared/components/header/header.component';
import { StatsCardComponent } from '../../shared/components/stats-card/stats-card.component';
import { ProgressBarComponent } from '../../shared/components/progress-bar/progress-bar.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    HeaderComponent,
    StatsCardComponent,
    ProgressBarComponent,
    NgClass
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
  protected selectedTags:string[]=[];

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
      (totalHours, project) => totalHours + project.budgetHours,
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

  // protected filterByTag(tag:string): void{
  //   const tagIndex=this.selectedTags.indexOf(tag);

  //   //removing tag if it was already selected
  //   if(tagIndex> -1){
  //     this.selectedTags.splice(tagIndex,1);
  //   }else{
  //     this.selectedTags.push(tag)
  //   }

  //   //no tags are selected= show everything
  //   if(this.selectedTags.length===0){
  //     this.filteredProjects=[...this.projects];
  //     return;
  //   }

  //   this.filteredProjects=this.projects.filter(
  //     project=>
  //       this.selectedTags.every(
  //         tag=>project.tags.includes(tag),
  //       ),
  //   );
  // }

  // protected get avaiableTags(): string[]{
  //   return[... new Set(
  //     this.projects.flatMap(project=> project.tags),
  //   )];
  // }

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
