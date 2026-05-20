import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loader',
  standalone: true,
  templateUrl: './loader.component.html',
  styleUrl: './loader.component.scss'
})
export class LoaderComponent {
  //size opiton for the loading and default is medium
  size = input<'small' | 'medium'| 'large'>('medium');
}
