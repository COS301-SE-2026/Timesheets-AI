/*
This covers tests for TimeEntryService. Same pattern as timer.service.spec.ts, one test per endpoint for the happy path, plus a single failure case since
handleError() is shared across every method, omg coverage again :(

Author: Zamokuhle Zwane
Date: 06 August 2026
*/

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TimeEntryService, TimeEntryResponse, TimeEntryRequest } from './time-entry.service';

describe('TimeEntryService', () => {
  let service: TimeEntryService;
  let httpMock: HttpTestingController;

  //match field for field
  const mockEntry: TimeEntryResponse = {
    id: 'entry-1',
    timesheetId: 'timesheet-1',
    workspaceMemberId: 'member-1',
    projectId: 'project-1',
    taskId: 'task-1',
    startTime: '2026-08-07T09:00:00',
    endTime: '2026-08-07T11:00:00',
    durationMinutes: 7200, // yes, seconds despite the field name(ik ik)
    entryType: 'MANUAL',
    description: 'Implemented login screen',
    isDeleted: false,
    createdAt: '2026-08-07T09:00:00',
    updatedAt: '2026-08-07T11:00:00',
  };

  const mockRequest: TimeEntryRequest = {
    projectId: 'project-1',
    taskId: 'task-1',
    startTime: '2026-08-07T09:00:00',
    endTime: '2026-08-07T11:00:00',
    durationSeconds: 7200,
    entryType: 'MANUAL',
    description: 'Implemented login screen',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TimeEntryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getMyEntries should GET /api/time-entries/me', () => {
    let result: TimeEntryResponse[] | undefined;
    service.getMyEntries().subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/time-entries/me');
    expect(req.request.method).toBe('GET');
    req.flush([mockEntry]);

    expect(result).toEqual([mockEntry]);
  });

  it('getEntry should GET /api/time-entries/{id}', () => {
    let result: TimeEntryResponse | undefined;
    service.getEntry('entry-1').subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/time-entries/entry-1');
    expect(req.request.method).toBe('GET');
    req.flush(mockEntry);

    expect(result).toEqual(mockEntry);
  });

  it('createEntry should POST /api/time-entries with the request body', () => {
    service.createEntry(mockRequest).subscribe();

    const req = httpMock.expectOne('/api/time-entries');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockEntry);
  });

  it('updateEntry should PUT the full object to /api/time-entries/{id}', () => {
    //this is a full replace
    service.updateEntry('entry-1', mockRequest).subscribe();

    const req = httpMock.expectOne('/api/time-entries/entry-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockEntry);
  });

  it('deleteEntry should DELETE /api/time-entries/{id}', () => {
    let completed = false;
    service.deleteEntry('entry-1').subscribe(() => (completed = true));

    const req = httpMock.expectOne('/api/time-entries/entry-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(completed).toBe(true);
  });

  it('should log and rethrow via the shared error handler when a request fails', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    let caught: unknown;

    service.getMyEntries().subscribe({ error: (err) => (caught = err) });

    const req = httpMock.expectOne('/api/time-entries/me');
    req.flush('server error', { status: 500, statusText: 'Internal Server Error' });

    expect(caught).toBeTruthy();
    expect(consoleSpy).toHaveBeenCalledWith(
      '[TimeEntryService] getMyEntries failed:',
      expect.objectContaining({ status: 500 }),
    );

    consoleSpy.mockRestore();
  });
});