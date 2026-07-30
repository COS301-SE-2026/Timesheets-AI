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
    initials: 'PA',
    description: 'Customer-facing web application rebuilding.',
    status: ProjectStatus.ACTIVE,
    budgetHours: 60,
    hourlyRate: 85,
    budgetCost: 5100,
    hoursLoggedLabel:'45h 18m',
    role: ProjectRole.DEVELOPER,
    teamMemberInitials: ['JD', 'SK', 'MR'],
    progressPercentageClamped: 76,
    detailLoaded: true,
    detailError: false,
    totalCost: 3850.5,
    hoursLogged: 45.3,
    // totalHours: 60,
    progressPercentage: 76,
    createdAt: '2026-07-01T08:00:00Z',
    updatedAt:'2026-07-28T14:30:00Z',
    myRole: ProjectRole.DEVELOPER,
    startDate: '2026-06-01T08:00:00Z',
    endDate: '2026-12-31T23:59:59Z',
    members:[
        {
            workspaceMemberId:'1',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john.doe@example.com',
            role: ProjectRole.DEVELOPER,
            hoursLogged: 18.5,
            // avatarColourClass: 'avatar-blue',
            joinedAt: '2026-06-15T09:00:00Z'
        },
        {
            workspaceMemberId:'2',
            firstName: 'Sara', 
            lastName:'Kim',
            email: 'sarakim@example.com',
            role: ProjectRole.MANAGER,
            hoursLogged: 26.1,
            // avatarColourClass: 'avatar-purple',
            joinedAt: '2026-06-15T09:00:00Z'
            
        },
        {
            workspaceMemberId:'3',
            firstName: 'Miguel', 
            lastName:'Ruiz',
            email: 'miguelr@example.com',
            role: ProjectRole.MANAGER,
            hoursLogged: 36.1,
            // avatarColourClass: 'avatar-purple',
            joinedAt: '2026-06-15T09:00:00Z'
        },
    ]
}