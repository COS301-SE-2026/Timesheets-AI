import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NEVER, throwError } from 'rxjs';
import { SettingsService } from './settings.services';
import { SettingsComponent } from './settings.component';
import { AuthService } from '../../core/services/auth.service';
import { IntegrationStatus } from './settings.model';

describe('SettingsComponent', () => {
  let component: SettingsComponent;
  let fixture: ComponentFixture<SettingsComponent>;
  let settingsService: SettingsService;
  let authService: AuthService;

  const github: IntegrationStatus={
    id: 'github',
    name: 'GitHub',
    description: 'test',
    icon: 'fa-brands fa-github',
    connected: false,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsComponent],
      providers:[
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SettingsComponent);
    component = fixture.componentInstance;
    settingsService= TestBed.inject(SettingsService);
    authService= TestBed.inject(AuthService);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('marks the integration as connecting while the url loads', ()=>{
    jest.spyOn(settingsService, 'getConnectUrl').mockReturnValue(NEVER);

    component.connectIntegration(github);

    expect(component.isSyncing('github')).toBe(true);
    expect(component.isSyncing('jira')).toBe(false);
  });

  it('resets the connecting state when connecting fails', ()=>{
    jest.spyOn(console, 'error').mockImplementation(()=> undefined);
    jest.spyOn(settingsService, 'getConnectUrl').mockReturnValue(throwError(()=> new Error('fail')));

    component.connectIntegration(github);

    expect(component.isSyncing('github')).toBe(false);
  });

  it('uses microsoft calendar when signed in with microsoft', ()=>{
    jest.spyOn(authService, 'getAuthProvider').mockReturnValue('MICROSOFT');
    const spy= jest.spyOn(settingsService, 'getConnectUrl').mockReturnValue(NEVER);

    component.connectIntegration({...github, id:'calendar'});

    expect(spy).toHaveBeenCalledWith('calendar', 'microsoft');
  });

  it('uses google calendar when signed in with google', ()=>{
    jest.spyOn(authService, 'getAuthProvider').mockReturnValue('GOOGLE');
    const spy= jest.spyOn(settingsService, 'getConnectUrl').mockReturnValue(NEVER);

    component.connectIntegration({...github, id:'calendar'});

    expect(spy).toHaveBeenCalledWith('calendar', 'google');
  });
});
