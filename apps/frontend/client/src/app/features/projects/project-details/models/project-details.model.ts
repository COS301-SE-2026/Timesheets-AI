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
    role: ProjectRole;
    joinedAt: string;
}
export interface ProjectDetails extends Project{
    totalCost: number;
    hoursLogged: number
    members: ProjectMember[];
    progressPercentage: number;
}

