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
        navogate: jest.fn(),
        url: '/dashboard'
    };

    // Mock authentication service
    const routeMock = {
        currentUser,
        logout: jest.fn(),
    };
})