import { Routes } from '@angular/router';
import { TimesheetsComponent } from './features/timesheets/timesheets.component';

export const routes: Routes = [
    { path: '', redirectTo:'timesheets', pathMatch:'full'},
    { path: 'timesheets', component: TimesheetsComponent}
];
