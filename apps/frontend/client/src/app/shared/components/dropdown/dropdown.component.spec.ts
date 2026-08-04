import { DropdownComponent } from './dropdown.component';
import { configureTestBed, createComponentTest } from '../../testing/component-test-helper';
import { ComponentFixture } from '@angular/core/testing';

createComponentTest(DropdownComponent, 'DropdownComponent');

describe('DropdownComponent', ()=> {
    let component: DropdownComponent;
    let fixture: ComponentFixture<DropdownComponent>;
    beforeEach(async ()=>{
        ({ component, fixture}=await configureTestBed(DropdownComponent))
    })

    //TC1: Fisrt state
    test('should be closed with no selection', ()=> {
        expect(component.isOpen()).toBe(false);
        expect(component.selectedOption()).toBe('');
    })

    //TC2: when dropdown is clicked
    test('should go to state isOpen when DropDown is called the closed', ()=> {
        //act
        component.toggleDropDown();
        //assert
        expect(component.isOpen()).toBe(true);

        //act
        component.toggleDropDown();
        //assert
        expect(component.isOpen()).toBe(false);
    });

    //TC4: dropdown is associated with and ID for verification
    test('should make a new unique dropdownId that start with "dropdownid-', ()=> {
        //assert
        expect(component.dropdownId).toContain('dropdown-');

        //assert
        expect(component.dropdownId.length).toBeGreaterThan(10);
    });

    //TC5: testing edge cases 
    test('should accept input that is disabled', ()=> {
        //arrange
        fixture.componentRef.setInput('disabled', true);
        fixture.detectChanges();
                
        //assert
        expect(component.disabled()).toBe(true);

    });
})