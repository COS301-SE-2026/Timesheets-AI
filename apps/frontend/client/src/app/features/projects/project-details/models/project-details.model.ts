/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: the data orginatizion area for the projects statistics.
Related Requirement: N/A
*/

import { Project } from "../../models/project.model";
import { ProjectRole } from "../../enums/project-role.enum";
// export { Project } from "../../models/project.model";

export interface ProjectMember{
    id: string;
    name: string;
    initials: string;
    role: ProjectRole;
    avatarColourClass: string;
}
export interface ProjectDetails extends Project{
    deadline?: string;
    members?: ProjectMember[];
}