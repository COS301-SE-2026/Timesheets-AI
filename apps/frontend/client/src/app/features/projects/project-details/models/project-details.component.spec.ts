/*
This file handles the tests for the ProjectDetailsComponent in the frontend
It uses Jest and Angular's testing utilities to ensure that the component behaves as expected under various scenarios
The tests cover component initialization, data binding, user interactions, and integration with services

Author: Zamokuhle Zwane
Date: 05 August 2026
*/


import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { ProjectDetailsComponent } from '../project-details.component';
import { ProjectDetailResponse } from '../../../../core/services/project.service';
import { TaskResponse } from '../../../../core/services/task.service';
import { ProjectMember } from '../models/project-details.model';
import { ProjectRole } from '../../enums/project-role.enum';


describe('ProjectDetailsComponent', () => {
  let fixture: ComponentFixture<ProjectDetailsComponent>;
  let component: any;
  let httpMock: HttpTestingController;
 
  const projectId = '00000000-0000-0000-0001-000000000040';
  const taskId = '00000000-0000-0000-0002-000000000070';

//MATCHES THE ProjectDetailResponse field for field
function makeProjectDetailResponse(
    overrides: Partial<ProjectDetailResponse> = {},
  ): ProjectDetailResponse {
    return {
      id: projectId,
      name: 'Momently Timesheets',
      description: 'Time tracking platform',
      status: 'ACTIVE',
      budgetHours: 100,
      hourlyRate: 50,
      budgetCost: 5000,
      totalCost: 1000,
      members: [
        {
          workspaceMemberId: 'member-1',
          firstName: 'Alice',
          lastName: 'Smith',
          email: 'alice@momentum.com',
          role: 'DEVELOPER',
          hoursLogged: 600, // 10 hours in minutes
          joinedAt: '2026-06-01T09:00:00',
        },
      ],
      hoursLogged: 3000, // 50 hours in minutes
      progressPercentage: 0, // ignored, mapToProjectDetails() recalculates this
      createdAt: '2026-06-01T09:00:00',
      updatedAt: '2026-06-01T09:00:00',
      ...overrides,
    };
  }

  function makeTaskResponse(overrides: Partial<TaskResponse> = {}): TaskResponse {
    return {
      id: taskId,
      projectId,
      projectName: 'Momently Timesheets',
      jiraTicketKey: 'MOM-201',
      parentTaskId: null,
      title: 'Wire the hours chart',
      description: null,
      status: 'IN_PROGRESS',
      priority: 'MEDIUM',
      estimatedHours: 4,
      actualHours: 1,
      assignedToName: 'Zamokuhle Zwane',
      assignedWorkspaceMemberId: 'member-2',
      dueDate: null,
      completedAt: null,
      isDeleted: false,
      deletedAt: null,
      createdAt: '2026-07-01T09:00:00',
      updatedAt: '2026-07-01T09:00:00',
      ...overrides,
    };
  }

  /*
  setup replaces the beforeEach function i typically use because
  this specific component reads from the activated route in the constructer
  */

  async function setup(routeId: string | null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ProjectDetailsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap(routeId ? { id: routeId } : {}),
            },
          },
        },
      ],
    })
      .overrideComponent(ProjectDetailsComponent, {
        set: { template: '<div></div>' },
      })
      .compileComponents();
 
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProjectDetailsComponent);
    component = fixture.componentInstance;
    // note: the constructor already called loadProject() and fired the GET
    // request by this point, TestBed.createComponent() runs the constructor
  }
 
  afterEach(() => {
    httpMock.verify();
  });

  it('should create', async () => {
    await setup(projectId);
    httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
    httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
    expect(component).toBeTruthy();
  });
 
  describe('route id handling', () => {
    it('should set error and skip the network call entirely when there is no id in the route', async () => {
      await setup(null);
 
      expect(component.loading()).toBe(false);
      expect(component.error()).toBe(true);
      httpMock.expectNone(`/api/projects/${projectId}`);
    });
  });

  describe('loadProject()', () => {
    it('should GET /api/projects/{id}, map the response, and then load the project tasks', async () => {
      await setup(projectId);
 
      expect(component.loading()).toBe(true);
 
      const projectReq = httpMock.expectOne(`/api/projects/${projectId}`);
      expect(projectReq.request.method).toBe('GET');
      projectReq.flush(makeProjectDetailResponse());
 
      expect(component.loading()).toBe(false);
      expect(component.error()).toBe(false);
      // this comes from mapToProjectDetails(), already unit tested directly in
      // project-mapper.spec.ts - here I'm just checking the component wired it in
      expect(component.project().name).toBe('Momently Timesheets');
      expect(component.project().hoursLogged).toBe(50); // 3000 minutes -> 50 hours
 
      const taskReq = httpMock.expectOne(`/api/tasks/project/${projectId}`);
      expect(taskReq.request.method).toBe('GET');
      taskReq.flush([makeTaskResponse()]);
 
      expect(component.tasks()).toHaveLength(1);
      expect(component.tasks()[0].title).toBe('Wire the hours chart');
    });
     it('should set error and stop loading when the project detail request fails, and never request tasks', async () => {
      await setup(projectId);
 
      const projectReq = httpMock.expectOne(`/api/projects/${projectId}`);
      projectReq.flush('not found', { status: 404, statusText: 'Not Found' });
 
      expect(component.loading()).toBe(false);
      expect(component.error()).toBe(true);
      httpMock.expectNone(`/api/tasks/project/${projectId}`);
    });
 
    it('should fall back to an empty task list without setting error when the task request fails', async () => {
      // matches the comment in the component: a broken task fetch should not
      // block the rest of the page, it should just render an empty table
      await setup(projectId);
 
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
 
      const taskReq = httpMock.expectOne(`/api/tasks/project/${projectId}`);
      taskReq.flush('server error', { status: 500, statusText: 'Internal Server Error' });
 
      expect(component.error()).toBe(false); // project itself still loaded fine
      expect(component.tasks()).toEqual([]);
    });
  });

  describe('computed values', () => {
    it('completionPercentage should be 0 when budgetHours is 0 (avoids divide by zero)', async () => {
      await setup(projectId);
      httpMock
        .expectOne(`/api/projects/${projectId}`)
        .flush(makeProjectDetailResponse({ budgetHours: 0 }));
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      expect(component.completionPercentage()).toBe(0);
    });
 
    it('completionPercentage should mirror progressPercentage once budgetHours is set', async () => {
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      // 50 corrected hours / 100 budgetHours = 50%, see calculateProgressPercentage() in project-mapper.ts
      expect(component.completionPercentage()).toBe(50);
    });
 
    it('remainingHours should be budgetHours minus hoursLogged', async () => {
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      expect(component.remainingHours()).toBe(50); // 100 budget - 50 logged
    });
 
    it('remainingHours should clamp to 0 when hoursLogged exceeds budgetHours', async () => {
      await setup(projectId);
      httpMock
        .expectOne(`/api/projects/${projectId}`)
        .flush(makeProjectDetailResponse({ budgetHours: 10, hoursLogged: 3000 })); // 50h logged, 10h budget
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      expect(component.remainingHours()).toBe(0);
    });
 
    it('hoursSummary should format as "{logged}h / {budget}h"', async () => {
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      expect(component.hoursSummary()).toBe('50h / 100h');
    });
  });

  describe('hoursChartData effect', () => {
    it('should rebuild the bar chart data from the project signal whenever it changes', async () => {
      /*
      the effect() in the constructor is what keeps the chart in sync, per the comment in the component ("ng2-charts doesnt pick up mutations"); it needs
      a change detection pass to flush, which fixture.detectChanges() does here safe only because the template was overridden to '<div></div>' above
      */
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
 
      fixture.detectChanges();
 
      expect(component.hoursChartData.datasets[0].data).toEqual([50, 100]);
      expect(component.hoursChartData.labels).toEqual(['Logged', 'Estimated']);
    });
  });
 
  describe('display helpers', () => {
    const member: ProjectMember = {
      workspaceMemberId: 'member-1',
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@momentum.co.za',
      hoursLogged: 10,
      hoursLoggedLabel: '10h 0m',
      role: ProjectRole.DEVELOPER,
      joinedAt: '2026-06-01T09:00:00',
    };
 
    beforeEach(async () => {
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
    });
 
    it('getMemberName should join first and last name', () => {
      expect(component.getMemberName(member)).toBe('Alice Smith');
    });
 
    it('getInitials should return the first letter of each of a member\'s names', () => {
      expect(component.getInitials(member)).toBe('AS');
    });
 
    it('getProjectInitials should take the first letter of up to the first two words of the project name, uppercased', () => {
      //project name from makeProjectDetailResponse() is "Momently Timesheets"
      expect(component.getProjectInitials()).toBe('MT');
    });
 
    it('getInitial should uppercase the first letter of every word in an arbitrary name', () => {
      expect(component.getInitial('Zamokuhle Zwane')).toBe('ZZ');
      expect(component.getInitial('Zamokuhle')).toBe('Z');
    });
 
    it('getAvatarColour should pick a colour deterministically from a member\'s first name length', () => {
      const colours = ['avatar-blue', 'avatar-purple', 'avatar-green', 'avatar-orange'];
      const expected = colours[member.firstName.length % colours.length];
      expect(component.getAvatarColour(member)).toBe(expected);
    });
 
    it('getAvatarColours should pick a colour deterministically from an arbitrary string length', () => {
      const colours = ['avatar-blue', 'avatar-purple', 'avatar-green', 'avatar-orange'];
      const expected = colours['Zamokuhle'.length % colours.length];
      expect(component.getAvatarColours('Zamokuhle')).toBe(expected);
    });
 
    it('formatStatus should map task status enum values to display labels', () => {
      expect(component.formatStatus('IN_PROGRESS')).toBe('In Progress');
      expect(component.formatStatus('TODO')).toBe('To Do');
      expect(component.formatStatus('BLOCKED')).toBe('Blocked');
      expect(component.formatStatus('DONE')).toBe('Done');
    });
 
    it('formatStatus should fall back to the raw value for an unrecognised status', () => {
      expect(component.formatStatus('SOMETHING_NEW')).toBe('SOMETHING_NEW');
    });
  });
 
  describe('setActiveTab()', () => {
    beforeEach(async () => {
      await setup(projectId);
      httpMock.expectOne(`/api/projects/${projectId}`).flush(makeProjectDetailResponse());
      httpMock.expectOne(`/api/tasks/project/${projectId}`).flush([]);
    });
 
    it('should default to the overview tab and switch to tasks on demand', () => {
      expect(component.activeTab()).toBe('overview');
 
      component.setActiveTab('tasks');
 
      expect(component.activeTab()).toBe('tasks');
    });
  });
});



