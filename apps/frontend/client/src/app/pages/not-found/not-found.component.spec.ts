import { TestBed } from '@angular/core/testing';
import { Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let component: NotFoundComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [
        {
          provide: Location,
          useValue: {
            back: jest.fn(),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {}, //fixes injector crash
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(NotFoundComponent);
    component = fixture.componentInstance;
  });

  it('should call location.back when goBack is triggered', () => {
    const location = TestBed.inject(Location);

    component.goBack();

    expect(location.back).toHaveBeenCalled();
  });
});