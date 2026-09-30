import { Component, OnInit } from "@angular/core";
import { SuggestionService} from './suggestion.service'
import { SuggestedWorkSession } from "./models/suggested.model";
import { AuthService } from "../../core/services/auth.service";
import { CommonModule } from '@angular/common';
import { ProjectService, ProjectResponse } from '../../core/services/project.service';

@Component({
    selector: 'app-suggestions',
    standalone: true,
    templateUrl: './suggestions.component.html',
    styleUrl: './suggestions.component.scss',
    imports: [CommonModule]
})

export class SuggestionsComponent implements OnInit {
    suggestions: SuggestedWorkSession[] = [];
    projects: ProjectResponse[] = [];

    loading = false;
    errorMessage = '';
    
    constructor(private authService: AuthService, private suggestionsService: SuggestionService, private projectService: ProjectService){}

    ngOnInit(): void {
        this.loadSuggestions();
        this.loadProjects();
    }

    private loadSuggestions(): void {
        const user = this.authService.currentUser();

        // workspace member id not found for current user 
        if (!user?.workspaceMemberId){
            return;
        }

        this.suggestionsService.getSuggestions(user.workspaceMemberId).subscribe({
            next: (suggestions) => {
                this.suggestions = suggestions;
            },
            error: (error) => {
                console.error('Failed to load suggestions', error);
            }
        });
    }

    // Helper method for formatting

    formatDate(date: string | Date): string {
        if (!date) return '';
        return new Date(date).toLocaleDateString();
    }

    formatTime(date: string | Date): string {
        if (!date) return '';
        return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    getSourceSummary(suggestion: SuggestedWorkSession): string {
        return suggestion?.explanation || 'Automated Suggestion';
    }


    // getting a list to show the availiable projects and its name instead of displaying 'Not project not found'
    getProjectName(projectId: string | null): string{
        if (!projectId){
            return 'No project assigned';
        }

        const project = this.projects.find(
            (project) => project.id === projectId
        );

        return project ? project.name : 'Unknown project';
    }

    assignProject(suggestion: SuggestedWorkSession, event: Event): void {
        const select = event?.target as HTMLSelectElement;
        const projectId = select?.value;

        if (!projectId){
            return;
        }

        console.log('Assigning project:', projectId);
        console.log('Suggestion:', suggestion.id);

        this.suggestionsService.edit(suggestion.id, {
            title: suggestion.title,
            projectId: projectId,
            taskId: suggestion.taskId || undefined,
            startTime: suggestion.startTime,
            endTime: suggestion.endTime,
            description: suggestion.description || undefined
        }).subscribe({
            next: (updatedSuggestion) => {
                const index = this.suggestions.findIndex(
                    (item) => item.id === updatedSuggestion.id
                );

                if (index !== -1) {
                    this.suggestions[index] = updatedSuggestion;
                }
            },
            error: (error) => {
                console.error('Failed to assign project:', error);
            }
        });
    }

    approveSuggestion(suggestion: SuggestedWorkSession): void {
    this.suggestionsService.approve(suggestion.id).subscribe({
        next: (response) => {
        this.suggestions = this.suggestions.filter(
            (item) => item.id !== suggestion.id
        );
        },
        error: (error) => {
        console.error('APPROVE: error =', error);
        }
    });
    }

    rejectSuggestion(suggestion: SuggestedWorkSession): void {
    this.suggestionsService.reject(suggestion.id).subscribe({
        next: () => {
        this.suggestions = this.suggestions.filter(
            (item) => item.id !== suggestion.id
        );
        },
        error: (error) => {
        console.error('Failed to reject suggestion:', error);
        }
    });
}

    loadProjects(): void {
        this.projectService.getProjects().subscribe({
            next: (projects) => {
                this.projects = projects;
                console.log('Projects:', projects);
            },
            error: (error) => {
                console.error('Failed to load projects', error);
            }
        });
    }

    generateSuggestions(): void {
    const user = this.authService.currentUser();

    if (!user?.workspaceMemberId) {
        console.error('Workspace member ID not found');
        return;
    }

    this.suggestionsService.generateSuggestions(
       user.workspaceMemberId,
    '2026-09-19T00:00:00',
    '2026-09-19T23:59:59'
    ).subscribe({
        next: (suggestions) => {
            console.log('Generated suggestions', suggestions);

            this.suggestions = suggestions;
        },
        error: (error) => {
            console.error('Generated error', error);
        }
    });
}
}

