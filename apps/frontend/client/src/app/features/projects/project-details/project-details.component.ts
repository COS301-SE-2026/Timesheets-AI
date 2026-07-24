/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A
*/

import { Component, OnInit, signal, computed } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HeaderComponent } from "../../../shared/components/header/header.component";
import { ProgressBarComponent } from "../../../shared/components/progress-bar/progress-bar.component";
import { StatusChipComponent } from "../../../shared/components/status-chip/status-chip.component";
import { ProjectMember } from "../project-details/models/project-details.model";
import { Project } from "../models/project.model";
import { PROJECTS } from "../mock/projects.mock";
import { ActivatedRoute } from "@angular/router";
@Component({
    selector: 'app-project-details',
    standalone: true,
    imports: [
        CommonModule,
        HeaderComponent,
        ProgressBarComponent,
        StatusChipComponent,
    ],
    templateUrl: './project-details.component.html',
    styleUrls: ['./project-details.component.scss']
})

export class ProjectDetailsComponent {
    constructor(
        private readonly route: ActivatedRoute,
    ){}

    protected readonly projectId= computed(()=>
        Number(this.route.snapshot.paramMap.get('id')),
    )
    
    protected readonly project= signal<Project>(PROJECTS[0]);

    protected readonly activeTab=signal<'overview' | 'tasks'>('overview');

    protected setActiveTab( tab: 'overview' | 'tasks'):void{
        this.activeTab.set(tab);
    }

    protected readonly completionPercentage= computed(()=>{
        const project= this.project();

        if(project.totalHours===0){
            return 0;
        }

        return Math.round(
            (project.hoursLogged / project.totalHours)*100,
        );
    });

    protected readonly remainingHours= computed(()=>{
        const project= this.project();

        return Math.max(
            0,
            project.totalHours- project.hoursLogged,
        );

    });

    protected readonly projectTitle= computed(
        ()=> this.project().name,
    );

    protected readonly projectStatus= computed(
        ()=> this.project().status,
    );

    protected readonly projectRole= computed(
        ()=> this.project().role,
    );

    protected readonly projectTags= computed(
        ()=> this.project().tags,
    );

    protected readonly projectMembers= computed(
        ()=> this.project().teamMembers,
    );
    
}