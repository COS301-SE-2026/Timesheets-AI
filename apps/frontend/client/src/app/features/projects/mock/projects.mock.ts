/**
 * MOCK OF PROJECTS
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-17
 * Purpose: temporay harcoded data that will be used for DEMO2
 * Related Requirement: N/A
 * Responsibilities:
 *  - once backend is ready to integrate
 *      just replace these with the API data
 */

import { Project } from '../models/project.model';
import { ProjectRole } from '../enums/project-role.enum';
import { ProjectStatus } from '../enums/project-status.enum';

export const PROJECTS: Project[] = [
  {
    id: 1,
    name: 'Project Alpha',
    client: 'TechCorp Inc.',
    description: 'Customer-facing web application rebuilding.',
    status: ProjectStatus.ACTIVE,
    hoursLogged: 45.3,
    totalHours: 60,
    role: ProjectRole.DEVELOPER,
    teamMembers: ['JD', 'SK', 'MR', 'CK', 'LS', 'KM'],
    tags: ['web', 'angular'],
  },
];
