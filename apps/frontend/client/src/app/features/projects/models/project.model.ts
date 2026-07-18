/**
 * PROJECTS MODEL
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-17
 * Purpose: is to define all the information every project should contain.
 * Related Requirement: N/A
 */

import { ProjectRole } from '../enums/project-role.enum';
import { ProjectStatus } from '../enums/project-status.enum';

export interface Project {
  id: number;
  name: string;
  client: string;
  description: string;
  status: ProjectStatus;
  hoursLogged: number;
  totalHours: number;
  role: ProjectRole;
  teamMembers: string[];
  tags: string[];
}
