/*
This file handles the conversion of the backend's ProjectResponse shape into
the front that Project's frontend expects
given the time crunch, we dont have hours logged but we do have calculateProjectTotalHours(), so this will remain frontend default
I will send the backend engineer a log so we can look into it in the next demo
Author: Zamokuhle Zwane
Date: 28/07/2026
*/
import { Project } from '../models/project.model';
import { ProjectRole } from '../enums/project-role.enum';
import { ProjectStatus } from '../enums/project-status.enum';
import {
  ProjectResponse,
  ProjectDetailResponse,
} from '../../../core/services/project.service';

const STATUS_MAP: Record<ProjectResponse['status'], ProjectStatus> = {
  ACTIVE: ProjectStatus.ACTIVE,
  ON_HOLD: ProjectStatus.ON_HOLD,
  COMPLETED: ProjectStatus.COMPLETED,
  ARCHIVED: ProjectStatus.ARCHIVED,
};

const ROLE_MAP: Record<ProjectResponse['myRole'], ProjectRole> = {
  ADMIN: ProjectRole.ADMIN,
  MANAGER: ProjectRole.MANAGER,
  DEVELOPER: ProjectRole.DEVELOPER,
};

function toInitials(name: string): string {
  const words = name.trim().split(/\s+/);
  return words
    .slice(0, 2)
    .map((w) => w.charAt(0).toUpperCase())
    .join('');
}

function toMemberInitials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
}

//This function turns decimal hours value(eg 540.66666) into 540h 4m
export function formatHoursMinutes(decimalHours: number): string {
  const totalMinutes = Math.round(decimalHours * 60);
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${hours}h ${minutes}m`;
}

//Clamps a percentage to 0-100 for anything that will drive a visual bars width
export function clampPercentage(value: number): number {
  return Math.min(100, Math.max(0, value));
}

//Builds the card shell from the list end before the detail data arrives
export function mapToProjectCard(res: ProjectResponse): Project {
  return {
    id: res.id,
    name: res.name,
    description: res.description ?? '',
    status: STATUS_MAP[res.status] ?? ProjectStatus.ACTIVE,
    role: res.myRole ? (ROLE_MAP[res.myRole] ?? null) : null,
    initials: toInitials(res.name),

    hoursLogged: null,
    hoursLoggedLabel: null,
    progressPercentage: null,
    progressPercentageClamped: null,
    teamMemberInitials: null,

    detailLoaded: false,
    detailError: false,
  };
}

//Applies the detail endpoint response to a existing card
export function applyProjectDetail(
  card: Project,
  detail: ProjectDetailResponse,
): void {
  card.hoursLogged = detail.hoursLogged;
  card.hoursLoggedLabel = formatHoursMinutes(detail.hoursLogged);
  card.progressPercentage = detail.progressPercentage;
  card.progressPercentageClamped = clampPercentage(detail.progressPercentage);
  card.teamMemberInitials = detail.members.map((m) =>
    toMemberInitials(m.firstName, m.lastName),
  );
  card.detailLoaded = true;
  card.detailError = false;
}

/*
Returns the logged in users own hoursLogged, we'll match by email rather than member id because Auth user carries email and id amd tehy arent
sent to frontend so the email is the only field both frontend and backend share
*/

export function extractMyHoursFromDetail(
  detail: ProjectDetailResponse,
  currentUserEmail: string,
): number {
  const me = detail.members.find(
    (m) => m.email.toLowerCase() === currentUserEmail.toLowerCase(),
  );
  return me?.hoursLogged ?? 0;
}
