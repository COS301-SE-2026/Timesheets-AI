/**
 * PROJECTS-TASK MODEL
 * ---------------------------------
 * Author: Cleopatra Kwenda
 * Date: 2026-07-30
 * Purpose: is to define all the information every project-task should contain.
 * Related Requirement: N/A
 */

export interface ProjectTask{
    id: string;
    projectId: string;
    title: string;
    description: string;
    status: string;
    priority: string;
    estimatedHours: number;
    actualHours: number;
    assignedToName: string;
    assignedWorkspaceMemberId: string;
    dueDate: string;
}