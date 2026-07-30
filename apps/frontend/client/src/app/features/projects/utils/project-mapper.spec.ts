/*
Author: Zamokuhle Zwane
Date: 29 July 2026
This file handles unit tests for the pure mapper functions in project-mapper.ts
covering the ProjectResponse into Project card shape, detail merge, hours formatting, and percentage clamping
*/
import {
  mapToProjectCard,
  applyProjectDetail,
  extractMyHoursFromDetail,
  formatHoursMinutes,
  clampPercentage,
} from './project-mapper';
import { ProjectRole } from '../enums/project-role.enum';
import { ProjectStatus } from '../enums/project-status.enum';
import {
  ProjectResponse,
  ProjectDetailResponse,
} from '../../../core/services/project.service';

// shared fixture constants, avoids raw numbers/strings repeating, across multiple describe blocks just to avoid Sonarqube flags
const FIXTURE_TIMESTAMP = '2026-07-26T13:30:16.182812';

const MOBILE_APP_BUDGET_HOURS = 500;
const MOBILE_APP_RATE = 75;
const MOBILE_APP_BUDGET_COST = 37500;

const THABANG_HOURS_LOGGED = 240;
const AMAHLE_HOURS_LOGGED = 0;
const TEAM_TOTAL_HOURS = 780.0666666666667; // -> "~780h 4m", tests minute rounding
const PROGRESS_OVER_BUDGET = 174.62; //deliberately over 100, tests clamping to 100%

const ENZOKUHLE_EMAIL = 'enzokuhle.khumalo@momentum.co.za';
const ENZOKUHLE_HOURS_LOGGED = 12.5;

describe('project-mapper', () => {
  describe('mapToProjectCard', () => {
    const baseResponse: ProjectResponse = {
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
    };

    it('should map status and role correctly', () => {
      const card = mapToProjectCard(baseResponse);
      expect(card.status).toBe(ProjectStatus.ACTIVE); //enum member, string comparison, toBe is correct here(sonarqube doesnt like it but it should be fine here)
      expect(card.role).toBe(ProjectRole.MANAGER);
    });

    it('should derive two-letter initials from a multi-word name', () => {
      const card = mapToProjectCard(baseResponse);
      expect(card.initials).toBe('MA');
    });

    it('should derive a single-letter initials from a one-word name gracefully', () => {
      const card = mapToProjectCard({ ...baseResponse, name: 'Backend' });
      expect(card.initials).toBe('B');
    });

    it('should default description to empty string if null', () => {
      const card = mapToProjectCard({ ...baseResponse, description: null });
      expect(card.description).toBe('');
    });

    it('should set role to null when myRole is null at runtime', () => {
      const card = mapToProjectCard({
        ...baseResponse,
        myRole: null as unknown as ProjectResponse['myRole'],
      });
      expect(card.role).toBeNull();
    });

    it('should leave detail-derived fields null and detailLoaded false before detail resolves', () => {
      const card = mapToProjectCard(baseResponse);
      expect(card.hoursLogged).toBeNull();
      expect(card.hoursLoggedLabel).toBeNull();
      expect(card.progressPercentage).toBeNull();
      expect(card.progressPercentageClamped).toBeNull();
      expect(card.teamMemberInitials).toBeNull();
      expect(card.detailLoaded).toBe(false);
      expect(card.detailError).toBe(false);
    });
  });

  describe('applyProjectDetail', () => {
    const detail: ProjectDetailResponse = {
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
          hoursLogged: THABANG_HOURS_LOGGED,
          joinedAt: FIXTURE_TIMESTAMP,
        },
        {
          workspaceMemberId: 'ws-2',
          firstName: 'Amahle',
          lastName: 'Dlamini',
          email: 'amahle.dlamini@momentum.co.za',
          role: 'MANAGER',
          hoursLogged: AMAHLE_HOURS_LOGGED,
          joinedAt: FIXTURE_TIMESTAMP,
        },
      ],
      hoursLogged: TEAM_TOTAL_HOURS,
      progressPercentage: PROGRESS_OVER_BUDGET,
      createdAt: FIXTURE_TIMESTAMP,
      updatedAt: FIXTURE_TIMESTAMP,
    };

    it('should set hoursLoggedLabel and progressPercentageClamped, and mark detailLoaded', () => {
      const card = mapToProjectCard({
        id: 'proj-1',
        name: 'Mobile App Development',
        description: '',
        status: 'ACTIVE',
        budgetHours: MOBILE_APP_BUDGET_HOURS,
        hourlyRate: MOBILE_APP_RATE,
        budgetCost: MOBILE_APP_BUDGET_COST,
        startDate: null,
        endDate: null,
        myRole: 'MANAGER',
        createdAt: '',
        updatedAt: '',
      });

      applyProjectDetail(card, detail);

      expect(card.hoursLogged).toBe(TEAM_TOTAL_HOURS);
      expect(card.hoursLoggedLabel).toBe('780h 4m');
      expect(card.progressPercentage).toBe(PROGRESS_OVER_BUDGET);
      expect(card.progressPercentageClamped).toBe(100);
      expect(card.teamMemberInitials).toEqual(['TS', 'AD']); // array, toEqual not toBe
      expect(card.detailLoaded).toBe(true);
      expect(card.detailError).toBe(false);
    });
  });

  describe('extractMyHoursFromDetail', () => {
    const detail: ProjectDetailResponse = {
      id: 'proj-1',
      name: '',
      description: '',
      status: 'ACTIVE',
      budgetHours: 0,
      hourlyRate: 0,
      budgetCost: 0,
      totalCost: 0,
      members: [
        {
          workspaceMemberId: 'ws-1',
          firstName: 'Enzokuhle',
          lastName: 'Khumalo',
          email: ENZOKUHLE_EMAIL,
          role: 'DEVELOPER',
          hoursLogged: ENZOKUHLE_HOURS_LOGGED,
          joinedAt: '',
        },
      ],
      hoursLogged: ENZOKUHLE_HOURS_LOGGED,
      progressPercentage: 50,
      createdAt: '',
      updatedAt: '',
    };

    it("should return the matching member's hoursLogged when the email matches exactly", () => {
      const result = extractMyHoursFromDetail(detail, ENZOKUHLE_EMAIL);
      expect(result).toBe(ENZOKUHLE_HOURS_LOGGED);
    });

    // so this test is directly exercising the toLowerCase call
    it('should match case-insensitively since the source lowercases both sides', () => {
      const result = extractMyHoursFromDetail(
        detail,
        ENZOKUHLE_EMAIL.toUpperCase(),
      );
      expect(result).toBe(ENZOKUHLE_HOURS_LOGGED);
    });

    it('should return 0 when no member matches the given email', () => {
      const result = extractMyHoursFromDetail(detail, 'nobody@momentum.co.za');
      expect(result).toBe(0);
    });
  });

  describe('formatHoursMinutes', () => {
    it('should format a whole number of hours with 0 minutes', () => {
      expect(formatHoursMinutes(6)).toBe('6h 0m');
    });

    it('should format a fractional value into hours and minutes', () => {
      expect(formatHoursMinutes(TEAM_TOTAL_HOURS)).toBe('780h 4m');
    });

    it('should show a sub-hour duration rather than swallowing it (34 minutes)', () => {
      expect(formatHoursMinutes(34 / 60)).toBe('0h 34m');
    });

    it('should round 59.94 minutes up to 1h 0m rather than showing 0h 60m', () => {
      expect(formatHoursMinutes(0.999)).toBe('1h 0m');
    });

    it('should format exactly 0 as "0h 0m"', () => {
      expect(formatHoursMinutes(0)).toBe('0h 0m');
    });
  });

  describe('clampPercentage', () => {
    it('should leave values within 0-100 unchanged', () => {
      expect(clampPercentage(0)).toBe(0);
      expect(clampPercentage(50)).toBe(50);
      expect(clampPercentage(100)).toBe(100);
    });

    it('should clamp values above 100 down to 100', () => {
      expect(clampPercentage(PROGRESS_OVER_BUDGET)).toBe(100);
    });

    it('should clamp negative values up to 0', () => {
      expect(clampPercentage(-5)).toBe(0);
    });
  });
});
