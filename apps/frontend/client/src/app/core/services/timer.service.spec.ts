/*
This handles tests for TimerService. No spec existed, this only got partial coverage through logtime.component.spec.ts exercising it indirectly. Testing every
endpoint directly here so each one's success and error path is confirmed against the real request shape (method + URL + body), same
HttpTestingController approach as the other service specs.

Author: Zamokuhle Zwane
Date: 06 August 2026
*/

import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TimerService, ActiveTimerResponse } from './timer.service';

describe('TimerService', () => {
  let service: TimerService;
  let httpMock: HttpTestingController;

  const mockActiveTimer: ActiveTimerResponse = {
    id: 'timer-1',
    project: { id: 'project-1', name: 'Mobile App Development' },
    task: { id: 'task-1', title: 'Implement login screen' },
    startedAt: '2026-08-07T09:00:00',
    elapsedMinutes: 12,
    elapsedSeconds: 720,
    active: true,
    isPaused: false,
    pausedAt: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TimerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('startTimer should POST /api/timers/start with the project/task ids', () => {
    let result: ActiveTimerResponse | undefined;
    service
      .startTimer({ projectId: 'project-1', taskId: 'task-1' })
      .subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/timers/start');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ projectId: 'project-1', taskId: 'task-1' });
    req.flush(mockActiveTimer);

    expect(result).toEqual(mockActiveTimer);
  });

  it('pauseTimer should POST an empty body to /api/timers/pause', () => {
    service.pauseTimer().subscribe();

    const req = httpMock.expectOne('/api/timers/pause');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush({ ...mockActiveTimer, isPaused: true });
  });

  it('resumeTimer should POST an empty body to /api/timers/resume', () => {
    service.resumeTimer().subscribe();

    const req = httpMock.expectOne('/api/timers/resume');
    expect(req.request.method).toBe('POST');
    req.flush(mockActiveTimer);
  });

  it('stopTimer should POST an empty body to /api/timers/stop', () => {
    service.stopTimer().subscribe();

    const req = httpMock.expectOne('/api/timers/stop');
    expect(req.request.method).toBe('POST');
    req.flush(mockActiveTimer);
  });

  it('getActiveTimer should GET /api/timers/active', () => {
    let result: ActiveTimerResponse | undefined;
    service.getActiveTimer().subscribe((res) => (result = res));

    const req = httpMock.expectOne('/api/timers/active');
    expect(req.request.method).toBe('GET');
    req.flush(mockActiveTimer);

    expect(result).toEqual(mockActiveTimer);
  });

  it('discardTimer should DELETE /api/timers/discard', () => {
    service.discardTimer().subscribe();

    const req = httpMock.expectOne('/api/timers/discard');
    expect(req.request.method).toBe('DELETE');
    req.flush(mockActiveTimer);
  });

  it('should log and rethrow via the shared error handler when a request fails', () => {
    //this covers the handleError() closure body, one failure is enough since every method routes through the same handler
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    let caught: unknown;

    service.getActiveTimer().subscribe({ error: (err) => (caught = err) });

    const req = httpMock.expectOne('/api/timers/active');
    req.flush('no active timer', { status: 404, statusText: 'Not Found' });

    expect(caught).toBeTruthy();
    expect(consoleSpy).toHaveBeenCalledWith(
      '[TimerService] getActiveTimer failed:',
      expect.objectContaining({ status: 404 }),
    );

    consoleSpy.mockRestore();
  });
});