// Author: Cleopatra Kwenda
// Date: 2026-08-18
// Purpose: has the http services that fetch
// the calander data from backend
// Related Requirements: N/A

import { Injectable, inject } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { from, Observable } from "rxjs";

import{
    AppEvent,
    CalendarProvider
}from './calendar.model';

