// Author: Cleopatra Kwenda
// Date: 2026-09-02
// Purpose: fetches the logged-in users identity
// such the role based permissions can work

import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable, of, delay } from "rxjs";
import { CurrentUser } from "./settings.model";

@Injectable({ providedIn: 'root'})
export class CurrentUserService{
    private http= inject(HttpClient);
    private readonly apiUrl='api/users/me';

    private readonly mockUser: CurrentUser= {
        id: 'user-1',
        firstName: 'Enzokuhle',
        lastName: 'Dlamini',
        role: 'ADMIN',
    };

    getCurrentUser(): Observable<CurrentUser>{
        return of(this.mockUser).pipe(delay(150));
    }

    
}