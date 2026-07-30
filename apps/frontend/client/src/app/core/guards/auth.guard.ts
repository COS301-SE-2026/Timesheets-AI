/*
This is a functional route guard, it'll redirect to /login if there's no valid token
this is the missing piece that makes the auth service's clear stale token, force relogin logic
i added it for functionality related to sidebar.
ref: https://angular.dev/guide/routing/common-router-tasks#milestone-5-route-guards

Author: Zamokuhle Zwane
Date: 26 July 2026
*/

import {inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn =() =>{
    const authService = inject(AuthService);
    const router = inject(Router);

    if(authService.isLoggedIn()) {
        return true;
    }

    router.navigate(['/login']);
    return false;
};