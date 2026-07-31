/*
This file tests ngOnInits load flow, so the kist must render immediately, details calls
fill in each card after, the stats as well. these are like unit tests

Author: Zamokuhle Zwane
Date: 29 July 2026
*/
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProjectsComponent } from './projects.component';
import { ProjectStatus } from './enums/project-status.enum';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import {
  ProjectResponse,
  ProjectDetailResponse,
} from '../../core/services/project.service';
import { Project } from './models/project.model';

//specifically typed instead of as any
interface ProjectsComponentInternal {
  projects: Project[];
  filteredProjects: Project[];
  loading: boolean;
  error: boolean;
  myTotalHoursLabel: string;
  myTotalHoursLoading: boolean;
  totalProjects: number;
  activeProjects: number;
  onHoldProjects: number;
  completedProjects: number;
  filterProjects(status: string): void;
  searchProjects(term: string): void;
}

function internal(component: ProjectsComponent): ProjectsComponentInternal {
  return component as unknown as ProjectsComponentInternal;
}

//these are shared fixture constants
const PROJECTS_ENDPOINT = '/api/projects';
const projectDetailEndpoint = (id: string) => `${PROJECTS_ENDPOINT}/${id}`;
const ALL_PROJECT_IDS = ['proj-1', 'proj-2', 'proj-3'];

const FIXTURE_TIMESTAMP = '2026-07-26T13:30:16.182812';
const USER_EMAIL = 'enzokuhle.khumalo@momentum.co.za';

const MOBILE_APP_BUDGET_HOURS = 500;
const MOBILE_APP_RATE = 75;
const MOBILE_APP_BUDGET_COST = 37500;

const BACKEND_API_BUDGET_HOURS = 300;
const BACKEND_API_RATE = 90;
const BACKEND_API_BUDGET_COST = 27000;

const DESIGN_SYSTEM_BUDGET_HOURS = 150;
const DESIGN_SYSTEM_RATE = 65;
const DESIGN_SYSTEM_BUDGET_COST = 9750;

const THABANG_HOURS_LOGGED = 240;
const THABANG_MINUTES_RAW = THABANG_HOURS_LOGGED * 60
const USER_HOURS_LOGGED = 540.0666666666667; // -> "540h 4m", tests minute rounding
const USER_MINUTES_RAW = USER_HOURS_LOGGED * 60; //32404
const KARABO_HOURS_LOGGED = 330;
const KARABO_MINUTES_RAW = KARABO_HOURS_LOGGED * 60;
const KEITUMETSE_MINUTES_RAW = 34
const PROJ1_TEAM_TOTAL_HOURS = 780.0666666666667;
const PROJ1_TEAM_TOTAL_MINUTES_RAW = PROJ1_TEAM_TOTAL_HOURS * 60; //46804
const PROJ1_PROGRESS_OVER_BUDGET = 156.01; // deliberately over 100, tests clamping

describe('ProjectsComponent', () => {
  let component: ProjectsComponent;
  let fixture: ComponentFixture<ProjectsComponent>;
  let httpMock: HttpTestingController;
  let authServiceMock: Pick<AuthService, 'currentUser'>;

  const currentUser: AuthUser = {
    id: 'user-1',
    email: USER_EMAIL,
    firstName: 'Enzokuhle',
    lastName: 'Khumalo',
    avatarUrl: null,
    emailVerified: true,
    roles: ['DEVELOPER'],
    mfaEnabled: false,
  };

  ///matches GET /api/projects, no role flitering happens client side
  const mockProjectList: ProjectResponse[] = [
    {
      id: 'proj-1',
      name: 'Mobile App Development',
      description: 'Building the React Native mobile application',
      status: 'ACTIVE',
      budgetHours: MOBILE_APP_BUDGET_HOURS,
      hourlyRate: MOBILE_APP_RATE,
      budgetCost: MOBILE_APP_BUDGET_COST,
      startDate: null,
      endDate: null,
      myRole: 'MANAGER',
      createdAt: FIXTURE_TIMESTAMP,
      updatedAt: FIXTURE_TIMESTAMP,
    },
    {
      id: 'proj-2',
      name: 'Backend API',
      description: 'Spring Boot REST API development',
      status: 'ACTIVE',
      budgetHours: BACKEND_API_BUDGET_HOURS,
      hourlyRate: BACKEND_API_RATE,
      budgetCost: BACKEND_API_BUDGET_COST,
      startDate: null,
      endDate: null,
      myRole: 'DEVELOPER',
      createdAt: FIXTURE_TIMESTAMP,
      updatedAt: FIXTURE_TIMESTAMP,
    },
    {
      id: 'proj-3',
      name: 'Design System',
      description: 'Building the company design system',
      status: 'ON_HOLD',
      budgetHours: DESIGN_SYSTEM_BUDGET_HOURS,
      hourlyRate: DESIGN_SYSTEM_RATE,
      budgetCost: DESIGN_SYSTEM_BUDGET_COST,
      startDate: null,
      endDate: null,
      myRole: 'DEVELOPER',
      createdAt: FIXTURE_TIMESTAMP,
      updatedAt: FIXTURE_TIMESTAMP,
    },
  ];
  function mockDetailFor(id: string): ProjectDetailResponse {
    const detailsById: Record<string, ProjectDetailResponse> = {
      'proj-1': {
        id: 'proj-1',
        name: 'Mobile App Development',
        description: 'Building the React Native mobile application',
        status: 'ACTIVE',
        budgetHours: MOBILE_APP_BUDGET_HOURS,
        hourlyRate: MOBILE_APP_RATE,
        budgetCost: MOBILE_APP_BUDGET_COST,
        totalCost: MOBILE_APP_BUDGET_COST,
        members: [
          {
            workspaceMemberId: 'ws-1',
            firstName: 'Thabang',
            lastName: 'Siduke',
            email: 'thabang.siduke@momentum.co.za',
            role: 'DEVELOPER',
            hoursLogged: THABANG_MINUTES_RAW,
            joinedAt: FIXTURE_TIMESTAMP,
          },
          {
            workspaceMemberId: 'ws-2',
            firstName: 'Enzokuhle',
            lastName: 'Khumalo',
            email: USER_EMAIL,
            role: 'DEVELOPER',
            hoursLogged: USER_MINUTES_RAW,
            joinedAt: FIXTURE_TIMESTAMP,
          },
        ],
        hoursLogged: PROJ1_TEAM_TOTAL_MINUTES_RAW,
        progressPercentage: PROJ1_PROGRESS_OVER_BUDGET,
        createdAt: FIXTURE_TIMESTAMP,
        updatedAt: FIXTURE_TIMESTAMP,
      },
      'proj-2': {
        id: 'proj-2',
        name: 'Backend API',
        description: 'Spring Boot REST API development',
        status: 'ACTIVE',
        budgetHours: BACKEND_API_BUDGET_HOURS,
        hourlyRate: BACKEND_API_RATE,
        budgetCost: BACKEND_API_BUDGET_COST,
        totalCost: BACKEND_API_BUDGET_COST,
        members: [
          {
            workspaceMemberId: 'ws-3',
            firstName: 'Karabo',
            lastName: 'Modise',
            email: 'karabo.modise@momentum.co.za',
            role: 'DEVELOPER',
            hoursLogged: KARABO_MINUTES_RAW,
            joinedAt: FIXTURE_TIMESTAMP,
          },
        ],
        hoursLogged: KARABO_MINUTES_RAW,
        progressPercentage: 110,
        createdAt: FIXTURE_TIMESTAMP,
        updatedAt: FIXTURE_TIMESTAMP,
      },
      'proj-3': {
        id: 'proj-3',
        name: 'Design System',
        description: 'Building the company design system',
        status: 'ON_HOLD',
        budgetHours: DESIGN_SYSTEM_BUDGET_HOURS,
        hourlyRate: DESIGN_SYSTEM_RATE,
        budgetCost: DESIGN_SYSTEM_BUDGET_COST,
        totalCost: DESIGN_SYSTEM_BUDGET_COST,
        members: [
          {
            workspaceMemberId: 'ws-4',
            firstName: 'Keitumetse',
            lastName: 'Motaung',
            email: 'keitumetse.motaung@momentum.co.za',
            role: 'DEVELOPER',
            hoursLogged: KEITUMETSE_MINUTES_RAW,
            joinedAt: FIXTURE_TIMESTAMP,
          },
        ],
        hoursLogged: KEITUMETSE_MINUTES_RAW,
        progressPercentage: 0.38,
        createdAt: FIXTURE_TIMESTAMP,
        updatedAt: FIXTURE_TIMESTAMP,
      },
    };
    return detailsById[id];
  }
  async function setup() {
    authServiceMock = {
      currentUser: signal<AuthUser | null>(currentUser),
    };
    await TestBed.configureTestingModule({
      imports: [ProjectsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ProjectsComponent);
    component = fixture.componentInstance;
  }
  //flushes the list endpoint and drains the detail calls it triggers, any test that flushes the list has to flush httpClientTestingModule's
  function flushProjectListAndDetails(
    detailIds: string[] = ALL_PROJECT_IDS,
  ): void {
    httpMock.expectOne(PROJECTS_ENDPOINT).flush(mockProjectList);
    detailIds.forEach((id) => {
      httpMock.expectOne(projectDetailEndpoint(id)).flush(mockDetailFor(id));
    });
  }

  afterEach(() => {
    httpMock.verify(); //catches any request that went unasserted
  });

  describe('initial load(ngOnInit', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges(); //triggers ngOnInit
    });

    it('should create', () => {
      flushProjectListAndDetails();
      expect(component).toBeTruthy();
    });

    it('should be loading before the list resolves, and stop loading once it does', () => {
      //checked before AND after the same flush, since loading only toggles off the list response not detail
      expect(internal(component).loading).toBe(true);
      flushProjectListAndDetails();
      expect(internal(component).loading).toBe(false);
    });

    it('should render all projects returned by the list endpoint, no client side role filtering', () => {
      flushProjectListAndDetails();
      expect(internal(component).projects).toHaveLength(3);
      expect(internal(component).filteredProjects).toHaveLength(3);
    });

    it('should set error true and loading false if the list calls fails', () => {
      httpMock
        .expectOne(PROJECTS_ENDPOINT)
        .flush('server error', { status: 500, statusText: 'Server Error' });

      expect(internal(component).error).toBe(true);
      expect(internal(component).loading).toBe(false);
    });

    it('should issue a detail call for every project in the list, not a subset', () => {
      flushProjectListAndDetails();
      expect(internal(component).projects.every((p) => p.detailLoaded)).toBe(
        true,
      );
      httpMock.verify();
    });
  });

  describe('detail resolution, per card fields', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges();
    });

    it('should format hoursLoggedLabel as "Xh Ym", not a raw decimal', () => {
      flushProjectListAndDetails();
      const card = internal(component).projects.find((p) => p.id === 'proj-1')!;
      expect(card.hoursLoggedLabel).toBe('780h 4m');
    });

    it('should clamp progressPercentageClamped to 100 while keeping the raw value uncapped', () => {
      flushProjectListAndDetails();
      const card = internal(component).projects.find((p) => p.id === 'proj-1')!;
      expect(card.progressPercentage).toBe(PROJ1_PROGRESS_OVER_BUDGET);
      expect(card.progressPercentageClamped).toBe(100);
    });

    it('should populate teamMembersInitials from firstName/lastName', () => {
      flushProjectListAndDetails();
      const card = internal(component).projects.find((p) => p.id === 'proj-1')!;
      expect(card.teamMemberInitials).toEqual(['TS', 'EK']); //array, toEqual not toBe(sign SonarQube!)
    });

    it('should show the 34-min case correctly rather than rounding it away', () => {
      flushProjectListAndDetails();
      const card = internal(component).projects.find((p) => p.id === 'proj-3')!;
      expect(card.hoursLoggedLabel).toBe('0h 34m');
    });

    it('should mard the card as detailError and default to 0 hrs if a single detail call fails without sinking all the other cards', () => {
      httpMock.expectOne(PROJECTS_ENDPOINT).flush(mockProjectList);
      httpMock
        .expectOne(projectDetailEndpoint('proj-1'))
        .flush('server error', { status: 500, statusText: 'Server Error' });
      httpMock
        .expectOne(projectDetailEndpoint('proj-2'))
        .flush(mockDetailFor('proj-2'));
      httpMock
        .expectOne(projectDetailEndpoint('proj-3'))
        .flush(mockDetailFor('proj-3'));

      const failedCard = internal(component).projects.find(
        (p) => p.id === 'proj-1',
      )!;
      expect(failedCard.detailError).toBe(true);
      expect(failedCard.detailLoaded).toBe(true);

      const okCard = internal(component).projects.find(
        (p) => p.id === 'proj-2',
      )!;
      expect(okCard.detailError).toBe(false);
      expect(okCard.detailLoaded).toBe(true);
    });
  });
  describe('myTotalHoursLabel', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges();
    });

    it('should sum only the logged in users own hours across projects matched by email', () => {
      flushProjectListAndDetails();
      //USER_EMAIL only appears as member on proj 1
      expect(internal(component).myTotalHoursLabel).toBe('540h 4m');
      expect(internal(component).myTotalHoursLoading).toBe(false);
    });

    it('should still resolve myTotalHoursLabel even when on call fails', () => {
      httpMock.expectOne(PROJECTS_ENDPOINT).flush(mockProjectList);
      httpMock
        .expectOne(projectDetailEndpoint('proj-1'))
        .flush('server error', { status: 500, statusText: 'Server Error' });
      httpMock
        .expectOne(projectDetailEndpoint('proj-2'))
        .flush(mockDetailFor('proj-2'));
      httpMock
        .expectOne(projectDetailEndpoint('proj-3'))
        .flush(mockDetailFor('proj-3'));
      //proj-1 fails
      expect(internal(component).myTotalHoursLabel).toBe('0h 0m');
      expect(internal(component).myTotalHoursLoading).toBe(false);
    });
  });

  describe('empty project list', () => {
    it('should not issue any detail calls and should report 0h 0m if the list is empty', async () => {
      await setup();
      fixture.detectChanges();

      httpMock.expectOne(PROJECTS_ENDPOINT).flush([]);

      expect(internal(component).projects).toHaveLength(0);
      expect(internal(component).myTotalHoursLabel).toBe('0h 0m');
      expect(internal(component).myTotalHoursLoading).toBe(false);
      httpMock.verify(); //confirms no stray calls fired
    });
  });

  describe('stats getter', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges();
    });
    it('should count total, active, on hold, and completed correctly', () => {
      flushProjectListAndDetails();
      //proj-1: ACTIVE, proj 2: ACTIVE, project: ON_HOLD, none COMPLETED
      expect(internal(component).totalProjects).toBe(3);
      expect(internal(component).activeProjects).toBe(2);
      expect(internal(component).onHoldProjects).toBe(1);
      expect(internal(component).completedProjects).toBe(0);
    });
  });

  describe('filterProjects', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges();
    });

    it('should filter to only active-status projects when filtered by ProjectStatus.ACTIVE', () => {
      //so we filter by the real enum here rather than a guessed string
      flushProjectListAndDetails();
      internal(component).filterProjects(ProjectStatus.ACTIVE);
      expect(internal(component).filteredProjects).toHaveLength(2); //proj-1, proj-2
    });

    it('should restore all projects when filtered by "All"', () => {
      flushProjectListAndDetails();
      internal(component).filterProjects(ProjectStatus.ON_HOLD);
      internal(component).filterProjects('All');
      expect(internal(component).filteredProjects).toHaveLength(3);
    });
  });
  describe('searchProjects', () => {
    beforeEach(async () => {
      await setup();
      fixture.detectChanges();
    });

    it('should search by project name, case-insensitively', () => {
      flushProjectListAndDetails();
      internal(component).searchProjects('backend');
      expect(internal(component).filteredProjects).toHaveLength(1);
      expect(internal(component).filteredProjects[0].name).toBe('Backend API');
    });

    it('should return an empty list for a search term matching nothing', () => {
      flushProjectListAndDetails();
      internal(component).searchProjects('nonexistent project xyz');
      expect(internal(component).filteredProjects).toHaveLength(0);
    });
  });
});
