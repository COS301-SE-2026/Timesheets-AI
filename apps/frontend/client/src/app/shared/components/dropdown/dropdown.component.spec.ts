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

    
})