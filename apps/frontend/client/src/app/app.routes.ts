import { Routes } from '@angular/router';
import { ComponentShowcaseComponent } from './pages/component-showcase/component-showcase.component';

export const routes: Routes = [
    { path: '', redirectTo:'timesheets', pathMatch:'full'},
    { path: '', component: ComponentShowcaseComponent}
];
