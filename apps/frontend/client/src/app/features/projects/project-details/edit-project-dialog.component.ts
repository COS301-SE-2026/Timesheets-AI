import { Component, inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { ProjectDetailResponse, ProjectService } from "../../../core/services/project.service";

@Component({
    standalone: true,
    imports: [FormsModule, MatDialogModule, MatButtonModule],
    template:`
        <h2 mat-dialog-title>Edit Project</h2>
    <form #form="ngForm" (ngSubmit)="save()">
        <mat-dialog-content>
            <div class="fields">
                <label class="wide">Project name <input name="name" [(ngModel)]="draft.name" required maxlength="255"/></label>
                <label class="wide">Description<textarea name="description" [(ngModel)]="draft.description" rows="3"></textarea></label>
                <label>Budget hours<input name="budgetHours" type="number" [(ngModel)]="draft.budgetHours" min="0.01" step="0.01" /></label>
                <label>Hourly rate<input name="hourlyRate" type="number" [(ngModel)]="draft.hourlyRate"  min="0.01" step="0.01" /></label>
                <label>Budget cost<input name="budgetCost" type="number" [(ngModel)]="draft.budgetCost"  min="0.01" step="0.01" /><label>
                <label>Start Date<input name="startDate" type="date" [(ngModel)]="draft.startDate" [required]="!!data.startDate" /></label>
                <label>End Date<input name="endDate" type="date" [(ngModel)]="draft.endDate" [min]="draft.startDate || ''" [required]="!!data.endDate" /></label>
            </div>    
        </mat-dialog-content>    
        
        `
})