import { SidebarComponent } from './sidebar.component';
import { AuthService } from '../../../core/services/auth.service';
import { createComponentTest } from '../../testing/component-test-helper';
import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';

// createComponentTest(SidebarComponent, 'SidebarComponent');

describe('SidebarCoponent', () => {
    let component: SidebarComponent;

    interface MockUser {
        firstName: string;
        lastName: string;
        roles: string[];
    }

    // MOCK authenticated user
    const currentUser = signal<MockUser | null>({
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
        expect(component.navItems()).toHaveLength(11);
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
    });

    // shouuld open and close the profile menu
    it('should toggle the menu state', () => {
        expect(component.isMenuOpen()).toBe(false);

        component.toggleMenu();
        expect(component.isMenuOpen()).toBe(true);

        component.toggleMenu();
        expect(component.isMenuOpen()).toBe(false);
    });

    // should call the logout service
    it('should logout the user', () => {
        component.onLogout();

        expect(authServiceMock.logout).toHaveBeenCalled();
    });

    // should return the current route
    it('should return the active route', () => {
        expect(component.activeRoute()).toBe('/dashboard');
    });

    // should open menu when clicking outside it
    it('should toggle menu when clicking outside the menu', () => {
        const element = document.createElement('div');
        jest.spyOn(element, 'closest').mockReturnValue(null);

        const event = {
            target: element,
        } as unknown as MouseEvent;

        component.onProfileClick(event);

        expect(component.isMenuOpen()).toBe(true);
    });

    // should return guest if user is not logged in
    it('should display Guest when there is no user', () => {
        currentUser.set(null);

        expect(component.displayName()).toBe('Guest');
    });

    // should return ? if user is missing
    it('should return ? initials when there is no user', () => {
        currentUser.set(null);

        expect(component.initials()).toBe('?');
    });

    // should return empty role if user is missing
    it('should return an empty role when there is no user', () => {
        currentUser.set(null);
        expect(component.displayRole()).toBe('');
    });

    // should support roles without ROLE_prefix
    it('should format roles without the ROLE_prefix', () => {
        currentUser.set({
            firstName: 'Jane',
            lastName: 'Doe',
            roles: ['MANAGER'],
        });

        expect(component.displayRole()).toBe('Manager');
    })

    // it should return empty role if roles array is empty
    it('should return an empty role when roles are empty', () => {
        currentUser.set({
            firstName: 'Jane',
            lastName: 'Smith',
            roles: [],
        });

        expect(component.displayRole()).toBe('');
    });

    // should not toggle when clicking inside the menu
    it('should not toggle menu when clicking inside the menu', () => {
        const element = document.createElement('div');

        jest.spyOn(element, 'closest').mockReturnValue(document.createElement('div'));

        const event = {
            target: element,
        } as unknown as MouseEvent;

        component.onProfileClick(event);

        expect(component.isMenuOpen()).toBe(false);
    })

    it('should return ? when both names are empty', () => {
        currentUser.set({
            firstName: '',
            lastName: '',
            roles: ['ROLE_ADMIN'],
        });

        expect(component.initials()).toBe('?');
    });

      it('should return only the last initial when the first name is empty', () => {
        currentUser.set({
            firstName: '',
            lastName: 'Doe',
            roles: ['ROLE_ADMIN'],
        });

        expect(component.initials()).toBe('D');
    });

      it('should return only the first initial when the last name is empty', () => {
        currentUser.set({
            firstName: 'John',
            lastName: '',
            roles: ['ROLE_ADMIN'],
        });

        expect(component.initials()).toBe('J');
    });

    // should test for null undefined fields

    it('should return ? when name fields are undefined', () => {
        currentUser.set({ roles: ['ROLE_ADMIN']} as unknown as MockUser);
        expect(component.initials()).toBe('?')
    });

    it('should return only the last initial  when firstName is undefined', () => {
        currentUser.set({ lastName: 'Doe', roles: []} as unknown as MockUser);
        expect(component.initials()).toBe('D')
    });

    it('should return first initial when the last name is undefined ', () => {
        currentUser.set({ firstName: 'John', roles: []} as unknown as MockUser);
        expect(component.initials()).toBe('J')
    });

    it('should return empty role when roles is undefined', () => {
        currentUser.set({ firstName: 'A', lastName: 'B'} as unknown as MockUser);
        expect(component.initials()).toBe('AB')
    });


});