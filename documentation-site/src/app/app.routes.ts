/**
 * Author: Cleopatra Kwenda
 * Date: 2026-07-13
 * Purpose: This file defines the routes for the Documentation website and is the central routing place. 
 * Related Requirement: N/A yet
 **/
import { Routes } from '@angular/router';
import {HomeComponent} from './features/home/home.component';
import {AboutComponent} from './features/about/about.component';
import { DocumentationComponent } from './features/documentation/documentation.component';

export const routes: Routes = [
    {
        path: '',
        component: HomeComponent
    },
    {
        path: 'about',
        component: AboutComponent
    },
    {
        path: 'documentation',
        component: DocumentationComponent
    },
    {
        path: '**',
        redirectTo: ''
    }
];
