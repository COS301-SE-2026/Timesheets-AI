import { SidebarComponent } from './sidebar.component';
import { AuthService } from '../../../core/services/auth.service';
import { createComponentTest } from '../../testing/component-test-helper';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';

// createComponentTest(SidebarComponent, 'SidebarComponent');

describe('SidebarCoponent', () => {
    let component: SidebarComponent;

    // MOCK authenticated user
    const currentUser = signal({
        firstName: 'John',
        lastName: 'Doe',
        roles: ['ROLE_ADMIN'],
    });

    // Mock angular route
    const routerMock = {
        navigate: jest.fn(),
        url: '/dashboard'
    };

    // Mock authentication service
    const authServiceMock = {
        currentUser,
        logout: jest.fn(),
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [SidebarComponent],
            providers: [
                { provide: Router, useValue: routerMock},
                { provide: AuthService, useValue: authServiceMock}
            ],
        });

        component = TestBed.createComponent(SidebarComponent).componentInstance;

        // Clear mock history
        jest.clearAllMocks();
    });

    // Component should be created
    it('should create', () => {
        expect(component).toBeTruthy();
    });

    // Load all sidebar navigation items
    it('should contain all navigation items', () => {
        expect(component.navItems().length).toBe(10);
    });

    // should return the users full name
    it('should return the display name', () => {
        expect(component.displayName()).toBe('John Doe');
    });

    // Should remove ROLE_ prefix from role
    it('should return the formatted display role', () => {
        expect(component.displayRole()).toBe('Admin');
    });

    // should return user initials
    it('should return user initials', () => {
        expect(component.initials()).toBe('JD');
    });

    // navigate to selected route
    it('should navigate when setActive is called', () => {
        component.setActive('/projects');

        expect(routerMock.navigate).toHaveBeenCalledWith(['/projects']);
    })
})