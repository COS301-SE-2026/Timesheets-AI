/**
 * MOCK OF PROJECTS
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-25
 * Purpose: temporay harcoded data that will be used for DEMO2
 * Related Requirement: N/A
 * Responsibilities:
 *  - once backend is ready to integrate
 *      just replace these with the API data
 */

import { ProjectDetails } from "../project-details/models/project-details.model";
import { ProjectRole } from "../enums/project-role.enum";
import { ProjectStatus } from "../enums/project-status.enum";

export const PROJECT_DETAIL: ProjectDetails= {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    name: 'Project Alpha',
    client: 'TechCorp Inc.',
    description: 'Customer-facing web application rebuilding.',
    status: ProjectStatus.ACTIVE,
    hoursLogged: 45.3,
    totalHours: 60,
    role: ProjectRole.DEVELOPER,
    teamMembers: ['John Doe', 'Sara Kim', 'Miguel Ruiz', 'Cleopatra Kwenda', 'Lerato Sibanda', 'Kgaugelo Matsena'],
    tags: ['web', 'angular'],
    initials: 'PA',
    deadline:'2026-05-31',
    members:[
        {
            id:'1',
            name: 'John Doe',
            initials: 'JD',
            role: ProjectRole.DEVELOPER,
            avatarColourClass: 'avatar-blue',
        },
        {
            id:'2',
            name: 'Sara Kim',
            initials: 'SK',
            role: ProjectRole.MANAGER,
            avatarColourClass: 'avatar-purple',
        },
        {
            id:'3',
            name: 'Miguel Ruiz',
            initials: 'MR',
            role: ProjectRole.DEVELOPER,
            avatarColourClass: 'avatar-green',
        },
    ]
}