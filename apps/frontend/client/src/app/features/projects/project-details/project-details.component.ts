/*
Author: Cleopatra Kwenda
Date: 2026-07-25
Purpose: Display all the necessary components using other project models
with only mock that should be eaisly integrated.
Related Requirement: N/A
*/

import { Component, OnInit, signal, computed } from "@angular/core";
import { CommonModule } from "@angular/common";
import { HeaderComponent } from "../../../shared/components/header/header.component";
import { ProgressBarComponent } from "../../../shared/components/progress-bar/progress-bar.component";
import { StatusChipComponent } from "../../../shared/components/status-chip/status-chip.component";
import { ProjectMember } from "../project-details/models/project-details.model";
import { Project } from "../models/project.model";
import { PROJECTS } from "../mock/projects.mock";
@Component({
    selector: 'app-project-details',
    standalone: true,
    imports: [
        CommonModule,
        HeaderComponent,
        ProgressBarComponent,
        StatusChipComponent,
    ],
    templateUrl: './project-details.component.html',
    styleUrls: ['./project-details.component.scss']
})

export class ProjectDetailsComponent {}