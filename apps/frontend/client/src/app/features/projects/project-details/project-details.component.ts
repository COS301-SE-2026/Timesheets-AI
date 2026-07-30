/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A
*/

import { Component, signal, computed } from "@angular/core";
import { CommonModule } from "@angular/common";
import { PROJECT_DETAIL } from "../mock/project-details.mock";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { ProjectDetails, ProjectMember } from "./models/project-details.model";
import { ProjectStatus } from "../enums/project-status.enum";
import { ProjectRole } from "../enums/project-role.enum";
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { PROJECT_TASK } from "../mock/project-task.mock";
import { ProjectTask } from "../models/project-task.model";

@Component({
    selector: 'app-project-details',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        BaseChartDirective,
    ],
    templateUrl: './project-details.component.html',
    styleUrls: ['./project-details.component.scss']
})

export class ProjectDetailsComponent {

    protected readonly ProjectStatus= ProjectStatus;
    protected readonly ProjectRole= ProjectRole;

    protected readonly tasks=signal<ProjectTask[]>(
        PROJECT_TASK
    );

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

        if(project.budgetHours===0){
            return 0;
        }

        return project.progressPercentage;
    });

    protected readonly remainingHours= computed(()=>{
        const project= this.project();

        return Math.max(
            0,
            project.budgetHours- (project.hoursLogged ?? 0),
        );

    });

    // protected readonly projectTitle= computed(
    //     ()=> this.project().name,
    // );

    // protected readonly projectStatus= computed(
    //     ()=> this.project().status,
    // );

    // protected readonly projectRole= computed(
    //     ()=> this.project().role,
    // );

    // protected readonly projectTags= computed(
    //     ()=> this.project().tags,
    // );

    // protected readonly projectMembers= computed(
    //     ()=> this.project().teamMembers,
    // );

    protected readonly hoursSummary= computed(()=>{
        const project= this.project();
        
        return `${project.hoursLogged}h / ${project.budgetHours}h`;
    });

    hoursChartData: ChartConfiguration<'bar'>['data']= {
        labels: ['Logged', 'Estimated'],
        datasets: [
            {
                data:[
                    this.project().hoursLogged,
                    this.project().budgetHours
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
                max: this.project().budgetHours,
                ticks:{
                    stepSize:10
                },
                border:{
                    display: false
                }
            }
        }
    };

    //helpers to replace what i removed
    protected getMemberName(member: ProjectMember): string{
        return `${member.firstName} ${member.lastName}`
    }

    protected getInitials(member: ProjectMember): string{
        return `${member.firstName[0]}${member.lastName[0]}`
    }

    protected getProjectInitials():string{
        return this.project()
            .name
            .split(' ')
            .map(word=> word[0])
            .join('')
            .substring(0, 2)
            .toUpperCase();
    }

    protected getInitial(name:string):string{
        return name
            .split(' ')
            .map(word=> word[0])
            .join('')
            .toUpperCase();
    }


    protected getAvatarColour(member: ProjectMember):string{
        const colours=[
            'avatar-blue',
            'avatar-purple',
            'avatar-green',
            'avatar-orange'
        ];

        return colours[
            member.firstName.length%colours.length
        ];
    }

    protected getAvatarColours(member: string):string{
        const colours=[
            'avatar-blue',
            'avatar-purple',
            'avatar-green',
            'avatar-orange'
        ];

        return colours[
            member.length%colours.length
        ];
    }

    protected formatStatus(status: string):string{
        switch(status){
            case 'IN_PROGRESS':
                return 'In Progress';
            case 'TODO':
                return 'To Do';
            case 'BLOCKED':
                return 'Blocked';
            case 'DONE':
                return 'Done';
            default:
                return status;
        }
    }
    
}