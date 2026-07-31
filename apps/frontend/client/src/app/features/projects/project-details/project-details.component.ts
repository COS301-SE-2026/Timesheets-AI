/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A

Patched: 30/07/2026 integration Zamokuhle Zwane
Intergrated project detail page replacing the hard coded details
*/

import { Component, signal, computed, inject, effect } from "@angular/core";
import { CommonModule } from "@angular/common";
//import { PROJECT_DETAIL } from "../mock/project-details.mock";
import { ActivatedRoute, RouterModule } from "@angular/router";
import { ProjectDetails, ProjectMember } from "./models/project-details.model";
import { ProjectStatus } from "../enums/project-status.enum";
import { ProjectRole } from "../enums/project-role.enum";
import { ChartConfiguration, ChartOptions } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
//import { PROJECT_TASK } from "../mock/project-task.mock";
import { ProjectTask } from "../models/project-task.model";
import { ProjectService } from "../../../core/services/project.service";
import { TaskService } from "../../../core/services/task.service";
import {mapToProjectDetails, mapToProjectTask} from "../utils/project-mapper";

//this is used to keep every template binding valid while real data is being loaded
const EMPTY_PROJECT_DETAILS: ProjectDetails = {
    id: '',
    name: '',
    initials: '',
    description: '',
    status: ProjectStatus.ACTIVE,
    hoursLogged: 0,
    hoursLoggedLabel: '0h 0m',
    role: null,
    teamMemberInitials: [],
    progressPercentage: 0,
    progressPercentageClamped: 0,
    detailLoaded: false,
    detailError: false,
    budgetHours: 0,
    hourlyRate: 0,
    budgetCost: 0,
    totalCost: 0,
    createdAt: '',
    updatedAt: '',
    myRole: ProjectRole.DEVELOPER,
    startDate: '',
    endDate: '',
    members: [],
};

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

    private readonly projectService = inject(ProjectService);
    private readonly taskService = inject(TaskService);

    protected readonly loading = signal<boolean>(true);
    protected readonly error = signal<boolean>(false);

    protected readonly tasks=signal<ProjectTask[]>([]);

    constructor(
        private readonly route: ActivatedRoute,
    ){
        this.loadProject();

        //this should keep the chart in sync whenever fresh project data comes in, because ng2-charts doesnt pick up mutations
        effect(() => {
            const project = this.project();
            
            this.hoursChartData = {
                labels: ['Logged', 'Estimated'],
                datasets: [
                    {
                        data: [project.hoursLogged, project.budgetHours],
                        backgroundColor: ['#2563eb', '#d1d5db'],
                        borderRadius: 10,
                        borderSkipped: false,
                        barThickness: 70,
                    },
                ],
            };

            this.hoursChartOptions = {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {display: false},
                    tooltip: {enabled: false},
                },
                scales: {
                    x: {
                        grid: {display: false},
                        border: {display: false},
                    },
                    y: {
                        beginAtZero: true,
                        max: Math.max(project.hoursLogged ?? 0, project.budgetHours) * 1.1 || undefined,
                        ticks: {stepSize: 10},
                        border: {display: false},
                    },
                },
            };
        });
    }

    protected readonly projectId= computed(()=>
        this.route.snapshot.paramMap.get('id') ?? '',
    )

    protected readonly project= signal<ProjectDetails>(
        EMPTY_PROJECT_DETAILS,
    )

    // protected readonly project= signal<Project>(PROJECTS[0]);

    protected readonly activeTab=signal<'overview' | 'tasks'>('overview');

    protected setActiveTab( tab: 'overview' | 'tasks'):void{
        this.activeTab.set(tab);
    }

    private loadProject(): void{
        const id = this.projectId();

        if(!id){
            this.loading.set(false);
            this.error.set(true);
            return;
        }

        this.loading.set(true);
        this.error.set(false);

        this.projectService.getProjectDetail(id).subscribe({
            next: (detail) => {
                this.project.set(mapToProjectDetails(detail));
                this.loading.set(false);
                this.loadTasks(id);
            },
            error: () => {
                this.loading.set(false);
                this.error.set(true);
            },
        });
    }

    private loadTasks(id:string): void{
        this.taskService.getTasksForProject(id).subscribe({
            next: (taskResponses) => {
                this.tasks.set(taskResponses.map(mapToProjectTask));
            },
            //if a task fails to load it shouldnt block the rest of page, it must just show an empty table
            error: () => {
                this.tasks.set([]);
            }
        });
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
                    0, 0
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