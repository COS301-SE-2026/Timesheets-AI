import { Component, inject } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from "@angular/material/dialog";
import { MatButtonModule } from "@angular/material/button";
import { ProjectDetailResponse, ProjectService } from "../../../core/services/project.service";

@Component({
    standalone: true,
    imports: [FormsModule, MatDialogModule, MatButtonModule],
    template: `
    <h2 mat-dialog-title>Edit Project</h2>
    <form #form="ngForm" (ngSubmit)="save()">
        <mat-dialog-content>
            <div class="fields">
                <label class="wide">Project name <input name="name" [(ngModel)]="draft.name" required maxlength="255"/></label>
                <label class="wide">Description<textarea name="description" [(ngModel)]="draft.description" rows="3"></textarea></label>
                <label class="wide">Status
                    <select name="status" [(ngModel)]="draft.status" required>
                        <option value="ACTIVE">Active</option>
                        <option value="ON_HOLD">On Hold</option>
                        <option value="COMPLETED">Completed</option>
                        <option value="ARCHIVED">Archived</option>
                    </select>
                </label>    
                <label>Budget hours<input name="budgetHours" type="number" [(ngModel)]="draft.budgetHours" min="0.01" step="0.01" /></label>
                <label>Hourly rate<input name="hourlyRate" type="number" [(ngModel)]="draft.hourlyRate"  min="0.01" step="0.01" /></label>
                <label>Budget cost<input name="budgetCost" type="number" [(ngModel)]="draft.budgetCost"  min="0.01" step="0.01" /></label>
                <label>Start Date<input name="startDate" type="date" [(ngModel)]="draft.startDate" [required]="!!data.startDate" /></label>
                <label>End Date<input name="endDate" type="date" [(ngModel)]="draft.endDate" [min]="draft.startDate || ''" [required]="!!data.endDate" /></label>
            </div>
            @if (invalidDates) { <p role="alert">End date must be on or after start date.</p> }
            @if (error) { <p role="alert">{{ error }}</p> }
        </mat-dialog-content>

        <mat-dialog-actions align="end">
            <button mat-button type="button" class="cancel-btn" [disabled]="saving" (click)="dialogRef.close()"> Cancel </button>
            <button mat-flat-button type="submit" class="save-btn" [disabled]="form.invalid || !draft.name.trim() || invalidDates || saving">{{ saving ? 'Saving...' : 'Save changes '}}</button>
        </mat-dialog-actions>
    </form>
    `,
  styles: [`
    .fields {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 18px;
        padding-top: 8px;
    }

    label {
        display: flex;
        flex-direction: column;
        gap: 8px;
        font-size: 14px;
    }

    input, textarea, select {
        box-sizing: border-box;
        width: 100%;
        padding: 10px;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        font: inherit;
    }

    .save-btn {
    background-color: var(--color-primary) !important;
    color: white !important;
    border-radius: 10px;
    padding: 0.6rem 1.25rem;
    font-weight: 600;
    }

    .save-btn:disabled {
        opacity: 0.5;
    }

    .wide {
        grid-column: 1 / -1;
    }

    p {
        color: #b42318;
    }

   .cancel-btn {
    background-color: #ffffff !important;
    color: #415673 !important;
    border: 1px solid #cbd5e1 !important;
    border-radius: 10px;
    padding: 0.6rem 1.25rem;
    font-weight: 600;
}

    @media (max-width: 500px) { 
        .fields {
            grid-template-columns: 1fr;
        }
    }   

    `],

})

export class EditProjectDialogComponent {
    readonly data = inject<ProjectDetailResponse>(MAT_DIALOG_DATA);
    readonly dialogRef = inject(MatDialogRef<EditProjectDialogComponent>);

    private readonly projects = inject(ProjectService);
    readonly draft = {
    status: this.data.status,
    name: this.data.name,
    description: this.data.description ?? '',
    budgetHours: this.data.budgetHours,
    hourlyRate: this.data.hourlyRate,
    budgetCost: this.data.budgetCost,
    startDate: this.data.startDate ?? '',
    endDate: this.data.endDate ?? '',
};

    saving= false;
    error= '';

    get invalidDates(): boolean {
        return !!(this.draft.startDate && this.draft.endDate && this.draft.endDate < this.draft.startDate);
    }

    save(): void {
        if (this.saving || !this.draft.name.trim() || this.invalidDates) return;
        this.saving = true;
        this.error = '';
        this.dialogRef.disableClose = true;
        this.projects.updateProject(this.data.id, {
            status: this.draft.status,
            name: this.draft.name.trim(), description: this.draft.description.trim(),
            budgetHours: this.draft.budgetHours ?? undefined, hourlyRate: this.draft.hourlyRate ?? undefined,
            budgetCost: this.draft.budgetCost ?? undefined,
            startDate: this.draft.startDate || undefined, endDate: this.draft.endDate || undefined,
        }).subscribe({
            next: () => this.dialogRef.close(true),
            error: () => {
                this.saving = false;
                this.dialogRef.disableClose = false;
                this.error = 'Could not save this project. Please try again.';
            },
        });
    }

}