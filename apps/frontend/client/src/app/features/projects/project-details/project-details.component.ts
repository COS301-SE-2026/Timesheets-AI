/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A
*/

import { Component, signal, computed } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HeaderComponent } from "../../../shared/components/header/header.component";
import { ProgressBarComponent } from "../../../shared/components/progress-bar/progress-bar.component";
import { StatusChipComponent } from "../../../shared/components/status-chip/status-chip.component";
import { PROJECT_DETAIL } from "../mock/project-details.mock";
import { ActivatedRoute } from "@angular/router";
import { ProjectDetails } from "./models/project-details.model";
import { RouterModule } from "@angular/router";
import { ProjectStatus } from "../enums/project-status.enum";
import { ProjectRole } from "../enums/project-role.enum";
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

@Component({
    selector: 'app-project-details',
    standalone: true,
    imports: [
        CommonModule,
        HeaderComponent,
        RouterModule,
        ProgressBarComponent,
        StatusChipComponent,
        BaseChartDirective,
    ],
    templateUrl: './project-details.component.html',
    styleUrls: ['./project-details.component.scss']
})

export class ProjectDetailsComponent {

    protected readonly ProjectStatus= ProjectStatus;
    protected readonly ProjectRole= ProjectRole;

    constructor(
        private readonly route: ActivatedRoute,
    ){}

    protected readonly projectId= computed(()=>
        Number(this.route.snapshot.paramMap.get('id')),
    )

    protected readonly project= signal<ProjectDetails>(
        PROJECT_DETAIL,
    )

    // protected readonly project= signal<Project>(PROJECTS[0]);

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

    protected readonly hoursSummary= computed(()=>{
        const project= this.project();
        
        return `${project.hoursLogged}h / ${project.totalHours}h`;
    });

    hoursChartData: ChartConfiguration<'bar'>['data']= {
        labels: ['Logged', 'Estimated'],
        datasets: [
            {
                data:[
                    this.project().hoursLogged,
                    this.project().totalHours
                ],

                backgroundColor:[
                    '#2563eb',
                    '#d1d5db'
                ],
                
                borderRadius:10,
                borderSkipped: false,
                barThickness:70
            }
        ]
    };

    hoursChartOptions: ChartOptions<'bar'>={
        responsive: true,
        maintainAspectRatio: false,
        plugins:{
            legend:{
                display: false
            },

            tooltip:{
                enabled:false
            }
        },

        scales:{
            x:{
                grid:{
                    display:false
                },
                border:{
                    display: false
                }
            },

            y:{
                beginAtZero:true,
                max: this.project().totalHours,
                ticks:{
                    stepSize:10
                },
                border:{
                    display: false
                }
            }
        }
    };
    
}