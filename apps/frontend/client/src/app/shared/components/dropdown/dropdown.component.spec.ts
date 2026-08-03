import { DropdownComponent } from './dropdown.component';
import { createComponentTest } from '../../testing/component-test-helper';
import { assert } from 'node:console';

createComponentTest(DropdownComponent, 'DropdownComponent');

describe('DropdownComponent', ()=> {
    const { component, fixture } = createComponentTest( DropdownComponent, 'DropdownComponent');

    //TC1: Fisrt state
    test('should be closed with no selection', ()=> {
        expect(component.isOpen()).toBe(false);
        expect(component.selectedOption()).toBe('');
    })

    //TC2: when dropdown is clicked
    test('should go to state isOpen when DropDown is called', ()=> {
        //act
        component.toggleDropDown();
        //assert
        expect(component.isOpen()).toBe(true);

        //act
        component.toggleDropDown();
        //assert
        expect(component.selectedOption()).toBe(false);
    });
})