/*
This file handles tests, were using httpTesting Controller instead of mocking it because Task is a httpclient
Uses Jest  Angular's testing utilities to ensure that the component behaves as expected under various scenarios

Author: Zamokuhle Zwane
Date: 05 August 2026
*/
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MyTasksComponent, Task } from './my-tasks.component';
import { TaskResponse } from '../../core/services/task.service';
import { Router } from '@angular/router';

describe('MyTasksComponent', () => {
  let component: MyTasksComponent;
  let fixture: ComponentFixture<MyTasksComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  //real shaped ids, that Backend uses
  const projectId = '00000000-0000-0000-0001-000000000040';
  const todoTaskId = '00000000-0000-0000-0002-000000000001';
  const doneTaskId = '00000000-0000-0000-0002-000000000002';
  const blockedTaskId = '00000000-0000-0000-0002-000000000003';

  //matches TaskResponse field for field
  function makeTaskResponse(overrides: Partial<TaskResponse>): TaskResponse {
    return {
      id: todoTaskId,
      projectId,
      projectName: 'Mobile App Development',
      jiraTicketKey: 'MOM-101',
      parentTaskId: null,
      title: 'Implement login screen',
      description: 'Build the login UI and wire it to AuthService',
      status: 'TODO',
      priority: 'HIGH',
      estimatedHours: 8,
      actualHours: 2,
      assignedToName: 'Zeze Zwane',
      assignedWorkspaceMemberId: 'member-1',
      dueDate: '2026-08-20',
      completedAt: null,
      isDeleted: false,
      deletedAt: null,
      createdAt: '2026-07-01T09:00:00',
      updatedAt: '2026-07-01T09:00:00',
      ...overrides,
    };
  }

  const mockTasks: TaskResponse[] = [
    makeTaskResponse({ id: todoTaskId, status: 'TODO', priority: 'HIGH' }),
    makeTaskResponse({
      id: doneTaskId,
      status: 'DONE',
      priority: 'LOW',
      title: 'Set up CI pipeline',
      jiraTicketKey: 'MOM-102',
      completedAt: '2026-07-10T09:00:00',
    }),
    makeTaskResponse({
      id: blockedTaskId,
      status: 'BLOCKED',
      priority: 'CRITICAL',
      title: 'Fix OAuth redirect',
      jiraTicketKey: 'MOM-103',
    }),
  ];

  function flushInitialRequests(tasksData: TaskResponse[] = mockTasks): void {
    fixture.detectChanges();
    
    const jiraStatusReq = httpMock.expectOne('/api/integrations/jira/status');
    expect(jiraStatusReq.request.method).toBe('GET');
    jiraStatusReq.flush({ connected: false });

    const tasksReq = httpMock.expectOne('/api/tasks/my-tasks');
    expect(tasksReq.request.method).toBe('GET');
    tasksReq.flush(tasksData);
    
    const projectsReq = httpMock.expectOne('/api/projects');
    expect(projectsReq.request.method).toBe('GET');
    projectsReq.flush([]);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyTasksComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
 
    fixture = TestBed.createComponent(MyTasksComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });
 
  afterEach(() => {
    //fails the test if a request was made that nothing expected, or an expected request was never made, it catches accidental extra/missing calls
    httpMock.verify();
  });

  it('should create', () => {
    flushInitialRequests([]);
    expect(component).toBeTruthy();
  });
 
  describe('loadTasks()', () => {
    it('should GET /api/tasks/my-tasks and map the response into Task objects', () => {
      fixture.detectChanges(); //ngOnInit(this initialises) fires loadTasks()
 
      //assert loading state was set before the response came back
      expect(component.isLoading()).toBe(true);
 
      const jiraStatusReq = httpMock.expectOne('/api/integrations/jira/status');
      expect(jiraStatusReq.request.method).toBe('GET');
      jiraStatusReq.flush({ connected: false });

      const tasksReq = httpMock.expectOne('/api/tasks/my-tasks');
      expect(tasksReq.request.method).toBe('GET');
      tasksReq.flush(mockTasks);
      
      const projectsReq = httpMock.expectOne('/api/projects');
      projectsReq.flush([]);

      expect(component.isLoading()).toBe(false);
      expect(component.loadError()).toBe(false);
      expect(component.tasks()).toHaveLength(3);
      expect(component.tasks()[0].title).toBe('Implement login screen');
    });

     it('should set loadError and stop loading when the request fails', () => {

      const jiraStatusReq = httpMock.expectOne('/api/integrations/jira/status');
      expect(jiraStatusReq.request.method).toBe('GET');
      jiraStatusReq.flush({ connected: false });


      // Arrange / Act
      fixture.detectChanges();
      const tasksReq = httpMock.expectOne('/api/tasks/my-tasks'); 
      // Assert
      // simulates a backend 500, same failure shape HttpErrorResponse produces
      // https://angular.dev/api/common/http/HttpErrorResponse
      tasksReq.flush('server error', { status: 500, statusText: 'Internal Server Error' });
 

      const projectsReq = httpMock.expectOne('/api/projects');
      projectsReq.flush([]);

      expect(component.isLoading()).toBe(false);
      expect(component.loadError()).toBe(true);
      expect(component.tasks()).toEqual([]);
    });
 
    it('should default projectName to "Unknown Project" and assignedToName to "Unassigned" when null', () => {

      const jiraStatusReq = httpMock.expectOne('/api/integrations/jira/status');
      expect(jiraStatusReq.request.method).toBe('GET');
      jiraStatusReq.flush({ connected: false });

      // this covers the ?? fallback logic in mapToTask(), which exists because of the GET /api/tasks/my-tasks 
      fixture.detectChanges();
      const tasksReq = httpMock.expectOne('/api/tasks/my-tasks');
      tasksReq.flush([
        makeTaskResponse({
          id: 'task-null-fields',
          projectName: null,
          assignedToName: null,
        }),
      ]);

      const projectsReq = httpMock.expectOne('/api/projects');
      projectsReq.flush([]);
 
      const task = component.tasks()[0];
      expect(task.projectName).toBe('Unknown Project');
      expect(task.assignedToName).toBe('Unassigned');
    });
  });
 
  describe('computed counts', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });
 
    it('activeCount should only count non-deleted TODO/IN_PROGRESS tasks', () => {
      expect(component.activeCount()).toBe(1); //just the TODO one
    });
 
    it('completedCount should only count non-deleted DONE tasks', () => {
      expect(component.completedCount()).toBe(1);
    });

    it('archivedCount should only count non-deleted BLOCKED tasks', () => {
      expect(component.archivedCount()).toBe(1);
    });
 
    it('totalCount should count all non-deleted tasks', () => {
      expect(component.totalCount()).toBe(3);
    });
 
    it('should exclude soft-deleted tasks from every count', () => {
      //reload with one of the three marked isDeleted
      component.tasks.set(
        mockTasks.map((t) =>
          t.id === todoTaskId ? { ...t, isDeleted: true } : t,
        ) as unknown as Task[],
      );
 
      expect(component.totalCount()).toBe(2);
      expect(component.activeCount()).toBe(0);
    });
  });

  describe('applyFilters()', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });
 
    it('should hide DONE and BLOCKED tasks by default (showCompleted/showArchived both false)', () => {
      //applyFilters() runs automatically after loadTasks() resolves
      expect(component.filteredTasks()).toHaveLength(1);
      expect(component.filteredTasks()[0].status).toBe('TODO');
    });
 
    it('should include DONE tasks once showCompleted is toggled on', () => {
      component.showCompleted.set(true);
      component.applyFilters();
 
      const statuses = component.filteredTasks().map((t) => t.status);
      expect(statuses).toContain('DONE');
      expect(statuses).not.toContain('BLOCKED');
    });
    it('should include BLOCKED tasks once showArchived is toggled on', () => {
      component.showArchived.set(true);
      component.applyFilters();
 
      const statuses = component.filteredTasks().map((t) => t.status);
      expect(statuses).toContain('BLOCKED');
    });
 
    it('should filter by exact status when a specific status is selected, ignoring the show flags', () => {
      component.selectedStatus.set('DONE');
      component.applyFilters();
 
      expect(component.filteredTasks()).toHaveLength(1);
      expect(component.filteredTasks()[0].status).toBe('DONE');
    });
 
    it('should filter by search query across title, projectName and jiraTicketKey', () => {
      component.showCompleted.set(true);
      component.showArchived.set(true);
      component.searchQuery.set('MOM-103');
      component.applyFilters();
 
      expect(component.filteredTasks()).toHaveLength(1);
      expect(component.filteredTasks()[0].jiraTicketKey).toBe('MOM-103');
    });
 
    it('should be case-insensitive and trim whitespace on search', () => {
      component.showCompleted.set(true);
      component.showArchived.set(true);
      component.searchQuery.set('  fix oauth  ');
      component.applyFilters();
 
      expect(component.filteredTasks()).toHaveLength(1);
      expect(component.filteredTasks()[0].id).toBe(blockedTaskId);
    });
  });
  describe('additional coverage', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });

    //(added these because the coverage is down)
    describe('onOverlayKeydown()', () => {
      it('should close the modal on Enter', () => {
        component.onViewTask(component.tasks()[0]);
        httpMock
          .expectOne(`/api/tasks/${todoTaskId}`)
          .flush(makeTaskResponse({ id: todoTaskId }));

        const event = {
          key: 'Enter',
          preventDefault: jest.fn(),
        } as unknown as KeyboardEvent;

        component.onOverlayKeydown(event);

        expect(event.preventDefault).toHaveBeenCalled();
        expect(component.isDetailOpen()).toBe(false);
      });

      it('should close the modal on Space', () => {
        component.onViewTask(component.tasks()[0]);
        httpMock
          .expectOne(`/api/tasks/${todoTaskId}`)
          .flush(makeTaskResponse({ id: todoTaskId }));

        const event = {
          key: ' ',
          preventDefault: jest.fn(),
        } as unknown as KeyboardEvent;

        component.onOverlayKeydown(event);

        expect(component.isDetailOpen()).toBe(false);
      });

      it('should do nothing on other keys', () => {
        component.onViewTask(component.tasks()[0]);
        httpMock
          .expectOne(`/api/tasks/${todoTaskId}`)
          .flush(makeTaskResponse({ id: todoTaskId }));

        const event = {
          key: 'Tab',
          preventDefault: jest.fn(),
        } as unknown as KeyboardEvent;

        component.onOverlayKeydown(event);

        expect(event.preventDefault).not.toHaveBeenCalled();
        expect(component.isDetailOpen()).toBe(true);
      });
    });

    it('formatDateTime should format a valid ISO datetime using en-ZA locale', () => {
      //this covers the happy path because formatDateTime(undefined) is already covered above
      const formatted = component.formatDateTime('2026-08-20T14:30:00');
      expect(formatted).toMatch(/2026/);
    });

    it('getStatusIcon should return the expected Font Awesome class for each status', () => {
      //font Awesome free solid/regular set already loaded app-wide, see PRIORITY_ICONS comment above(atp im trying to get coverage up)
      expect(component.getStatusIcon('TODO')).toBe('fa-regular fa-circle');
      expect(component.getStatusIcon('IN_PROGRESS')).toBe(
        'fa-regular fa-circle-check',
      );
      expect(component.getStatusIcon('DONE')).toBe('fa-solid fa-check-circle');
      expect(component.getStatusIcon('BLOCKED')).toBe('fa-solid fa-ban');
    });
  });

  describe('event handlers', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });
 
    it('onSearchChange should update searchQuery and re-run applyFilters', () => {
      const input = document.createElement('input');
      input.value = 'login';
      const event = { target: input } as unknown as Event;
 
      component.onSearchChange(event);
 
      expect(component.searchQuery()).toBe('login');
      expect(component.filteredTasks()).toHaveLength(1);
    });
    /*
    okay so test failed becaise <select only accepts .value = 'X' if a matching <option>
    so an empty mock <selct silently keeps value === ''
    */
    it('onStatusFilterChange should update selectedStatus and re-run applyFilters', () => {
      const select = { value: 'BLOCKED' } as unknown as HTMLSelectElement;
      const event = { target: select } as unknown as Event;
 
      component.onStatusFilterChange(event);
 
      expect(component.selectedStatus()).toBe('BLOCKED');
      expect(component.filteredTasks()[0].status).toBe('BLOCKED');
    });

    it('onToggleCompleted should flip showCompleted and re-filter', () => {
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = true;
      const event = { target: checkbox } as unknown as Event;
 
      component.onToggleCompleted(event);
 
      expect(component.showCompleted()).toBe(true);
    });
 
    it('onToggleArchived should flip showArchived and re-filter', () => {
      const checkbox = document.createElement('input');
      checkbox.type = 'checkbox';
      checkbox.checked = true;
      const event = { target: checkbox } as unknown as Event;
 
      component.onToggleArchived(event);
 
      expect(component.showArchived()).toBe(true);
    });

    it('navigateToProject should call router.navigate with the projects route', () => {
      /*
      spying on the real Router instead of a fake, same approach used for AuthService in timesheets.component.spec.ts
      where a fake was only needed because that service touches localStorage, Router does not, so a spy is enough
      */
      const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
 
      component.navigateToProject(projectId);
 
      expect(navigateSpy).toHaveBeenCalledWith(['/projects', projectId]);
    });
  });
 
  describe('onStatusChange()', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });
 
    it('should update the task status locally without hitting the network', () => {
      //I flagged in the component comments: there is no PATCH /api/tasks/{id}/status endpoint yet, ill let Nyasha know
      component.onStatusChange(component.tasks()[0], 'DONE');
 
      const updated = component.tasks().find((t) => t.id === todoTaskId);
      expect(updated?.status).toBe('DONE');
      httpMock.expectNone('/api/tasks/' + todoTaskId + '/status');
    });
  });

   describe('task detail modal (GET /api/tasks/{taskId})', () => {
    beforeEach(() => {
      flushInitialRequests(mockTasks);
    });
 
    it('onViewTask should open the modal and load the full task record', () => {
      component.onViewTask(component.tasks()[0]);
 
      expect(component.isDetailOpen()).toBe(true);
      expect(component.isDetailLoading()).toBe(true);
 
      const req = httpMock.expectOne(`/api/tasks/${todoTaskId}`);
      expect(req.request.method).toBe('GET');
      req.flush(makeTaskResponse({ id: todoTaskId }));
 
      expect(component.isDetailLoading()).toBe(false);
      expect(component.selectedTask()?.id).toBe(todoTaskId);
    });
 
    it('onViewTask should set detailError and stop loading when the request fails', () => {
      component.onViewTask(component.tasks()[0]);
 
      const req = httpMock.expectOne(`/api/tasks/${todoTaskId}`);
      req.flush('not found', { status: 404, statusText: 'Not Found' });
 
      expect(component.isDetailLoading()).toBe(false);
      expect(component.detailError()).toBe(
        'Could not load task details, please try again.',
      );
    });
 
    it('closeTaskDetail should reset isDetailOpen, selectedTask and detailError', () => {
      component.onViewTask(component.tasks()[0]);
      const req = httpMock.expectOne(`/api/tasks/${todoTaskId}`);
      req.flush(makeTaskResponse({ id: todoTaskId }));
 
      component.closeTaskDetail();
 
      expect(component.isDetailOpen()).toBe(false);
      expect(component.selectedTask()).toBeNull();
      expect(component.detailError()).toBeNull();
    });
    it('onEscapeKey should close the modal only if it is currently open', () => {
      component.onEscapeKey();
      expect(component.isDetailOpen()).toBe(false); // was already closed, stays closed
 
      component.onViewTask(component.tasks()[0]);
      httpMock.expectOne(`/api/tasks/${todoTaskId}`).flush(makeTaskResponse({ id: todoTaskId }));
 
      component.onEscapeKey();
      expect(component.isDetailOpen()).toBe(false);
    });
 
    it('onOverlayClick should close the modal only when the overlay itself was clicked', () => {
      component.onViewTask(component.tasks()[0]);
      httpMock.expectOne(`/api/tasks/${todoTaskId}`).flush(makeTaskResponse({ id: todoTaskId }));
 
      const overlay = document.createElement('div');
      const inner = document.createElement('div');
      overlay.appendChild(inner);
 
      //click on the inner content should NOT close it
      component.onOverlayClick({ target: inner, currentTarget: overlay } as unknown as MouseEvent);
      expect(component.isDetailOpen()).toBe(true);
 
      //click on the overlay itself should close it
      component.onOverlayClick({ target: overlay, currentTarget: overlay } as unknown as MouseEvent);
      expect(component.isDetailOpen()).toBe(false);
    });
  });

  describe('formatting helpers', () => {
    beforeEach(() => {
      flushInitialRequests([]);
    });
 
    it('formatDate should return "-" for undefined and an invalid date string', () => {
      expect(component.formatDate(undefined)).toBe('-');
      expect(component.formatDate('not-a-date')).toBe('-');
    });
 
    it('formatDate should format a valid ISO date using en-ZA locale', () => {
      //Intl.DateTimeFormat / toLocaleDateString reference:(it caused time related inaccuracies)
      const formatted = component.formatDate('2026-08-20');
      expect(formatted).toMatch(/2026/);
    });
 
    it('formatDateTime should return "-" for undefined', () => {
      expect(component.formatDateTime(undefined)).toBe('-');
    });
 
    it('trackByTaskId should return the task id', () => {
      const task: Task = {
        id: 'abc-123',
        title: 't',
        projectName: 'p',
        projectId: 'pid',
        status: 'TODO',
        priority: 'LOW',
        estimatedHours: 0,
        actualHours: 0,
        assignedToName: 'a',
        createdAt: '',
        updatedAt: '',
        isDeleted: false,
      };
      expect(component.trackByTaskId(0, task)).toBe('abc-123');
    });
 
    it('getStatusClass and getPriorityClass should return the expected badge classes', () => {
      expect(component.getStatusClass('DONE')).toBe('status-badge status-done');
      expect(component.getPriorityClass('CRITICAL')).toBe(
        'priority-badge priority-critical',
      );
    });
  });
});
