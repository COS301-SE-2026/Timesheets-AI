/**
 * PROJECTS MODEL
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-17
 * Purpose: is to define all the information every project should contain.
 * Related Requirement: N/A
 *
 * Patched: Zamokuhle Zwane, 28/07/2026
 * Wired GET /api/projects and /api/porjects/id and dropped clent and tags field since
 * there's currently no backend source for those yet, now hoursLogged/progressPercentage come from the detail endpoint and load
 * after the initial card renders
 */

import { ProjectRole } from '../enums/project-role.enum';
import { ProjectStatus } from '../enums/project-status.enum';

export interface Project {
  id: string; //Backend UUID, kept as string now
  name: string;
  description: string;
  status: ProjectStatus;
  hoursLogged: number | null; //team total hours logged
  hoursLoggedLabel: string | null; //this will reformat the hours into something like 540h 4m instead of 540.4
  role: ProjectRole | null; //in backend role can be null
  teamMemberInitials: string[] | null;
  progressPercentage: number | null; //hoursLogged/bugdetHours *100
  progressPercentageClamped: number | null; //must be from 0-100, for the progress bar
  detailLoaded: boolean;
  detailError: boolean;
  //will ask nyasha to add edpoint i guess
  initials: string;
}
