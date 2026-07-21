import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';

export async function configureTestBed<T>(componentType: Type<T>): Promise<{ component: T; fixture: ComponentFixture<T> }> {
  await TestBed.configureTestingModule({
    imports: [componentType]
  }).compileComponents();

  const fixture = TestBed.createComponent(componentType);
  const component = fixture.componentInstance;
  fixture.detectChanges();

  return { component, fixture };
}

export function createComponentTest<T>(componentType: Type<T>, componentName: string) {
  describe(componentName, () => {
    let component: T;

    beforeEach(async () => {
      ({ component } = await configureTestBed(componentType));
    });

    it('should create', () => {
      expect(component).toBeTruthy();
    });
  });
}
