/**
 * MOCK OF PROJECTS-TASK
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-30
 * Purpose: temporay harcoded data that will be used for DEMO2
 * Related Requirement: N/A
 * Responsibilities:
 *  - once backend is ready to integrate
 *      just replace these with the API data
 */

import { ProjectTask } from "../models/project-task.model";
import { PROJECT_DETAIL } from "./project-details.mock";

export const PROJECT_TASK: ProjectTask[]=[
    {
        id: '1',
        projectId: PROJECT_DETAIL.id,
        title: 'Design login flow',
        description: '',
        status: 'IN-PROGRESS',
        priority: 'HIGH',
        estimatedHours: 8,
        actualHours: 6,
        assignedToName: 'John Doe',
        assignedWorkspaceMemberId: '1',
        dueDate: '2026-08-01'
    },
    {
        id: '2',
        projectId: PROJECT_DETAIL.id,
        title: 'Implement OAuth callback',
        description: '',
        status: 'TODO',
        priority: 'MEDIUM',
        estimatedHours: 5,
        actualHours: 0,
        assignedToName: 'Sara Kim',
        assignedWorkspaceMemberId: '2',
        dueDate: '2026-08-05'
    },
    {
        id: '3',
        projectId: PROJECT_DETAIL.id,
        title: 'Build Dashboard',
        description: '',
        status: 'DONE',
        priority: 'LOW',
        estimatedHours: 12,
        actualHours: 11,
        assignedToName: 'Miguel Ruiz',
        assignedWorkspaceMemberId: '3',
        dueDate: '2026-08-08'
    }
]