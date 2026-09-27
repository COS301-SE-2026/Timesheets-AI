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
            @if (invalidDate) { <p role="alert">End date must be on or after start date.</p> }
            @if (error) { <p role="alert">{{ error }}</p>
        </mat-dialog-content>

        <mat-dialog-actions align="end">
            <button mat-button type="button" [disabled]="saving" (click)="dialogRef..close()"> Cancel </button>
            <button mat-flat=button type="submit" [disabled]="form.invalid || !draft.name.trim() || invalidDates || saving">{{ saving ? 'Saving...' : 'Save changes '}}</button>
        </mat-dialog-actions>
    </form>
    `,
  styles: [`
    .fields {
        display: grid;
        grid-template-columns: repeat(2, minmax:(0, 1fr));
        gap: 18px;
        padding-top: 8px;
    }

    label {
        display: flex;
        flex-direction: column;
        gap: 8px;
        font-size: 14px;
    }

    input, textarea {
        box-sizing: border-box;
        width: 100%;
        padding: 10px;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        font: inherit;
    }

    .wide {
        grid-column: 1 / -1;
    }

    p {
        color: #b42318;
    }

    @media (max-width: 500px) { 
        .fields {
            grid-template-columns: 1fr;
        }
    }   

    `]

})