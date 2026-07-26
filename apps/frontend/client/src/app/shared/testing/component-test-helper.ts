import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Type, Provider } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

//added provideHttpClient()/provideHttpClient by default since authservice inject a service that needs HttpCliet even when the componennt itself never calls http
//directly

export async function configureTestBed<T>(
  componentType: Type<T>,
  extraProviders: Provider[] = [],
): Promise<{ component: T; fixture: ComponentFixture<T> }> {
  await TestBed.configureTestingModule({
    imports: [componentType],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      ...extraProviders,
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(componentType);
  const component = fixture.componentInstance;
  fixture.detectChanges();

  return { component, fixture };
}

export function createComponentTest<T>(
  componentType: Type<T>,
  componentName: string,
  extraProviders: Provider[] = [],
) {
  describe(componentName, () => {
    let component: T;
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    let fixture: ComponentFixture<T>;

    beforeEach(async () => {
      ({ component, fixture } = await configureTestBed(
        componentType,
        extraProviders,
      ));
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });
}
