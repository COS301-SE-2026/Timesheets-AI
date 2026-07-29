import { ComponentFixture, TestBed } from '@angular/core/testing';
import { VerifyEmailComponent } from './verify-email.component';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

describe('VerifyEmailComponent', () => {
  let component: VerifyEmailComponent;
  let fixture: ComponentFixture<VerifyEmailComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]), 
        {
          provide: AuthService
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VerifyEmailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
