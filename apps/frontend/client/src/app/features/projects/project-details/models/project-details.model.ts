/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: the data orginatizion area for the projects statistics.
Related Requirement: N/A
*/

import { Project } from "../../models/project.model";
import { ProjectRole } from "../../enums/project-role.enum";

export interface ProjectMember{
    workspaceMemberId: string;
    firstName: string;
    lastName: string;
    email: string;
    hoursLogged: number;
    /** making this to accept null for now until frontend team fixs the mock files with mock data */
    hoursLoggedLabel?: string;
    role: ProjectRole;
    joinedAt: string;
}
export interface ProjectDetails extends Project{
    budgetHours: number;
    hourlyRate: number;
    budgetCost: number
    totalCost: number;
    createdAt: string;
    updatedAt:string;
    myRole: ProjectRole;
    startDate: string;
    endDate: string;
    members: ProjectMember[];
    progressPercentage: number;
}

