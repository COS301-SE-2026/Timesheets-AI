/** 
 * Author: Cleopatra Kwenda
 * Date: 2024-07-15
 * Purpose: This component displays the animated hourglass found on the 
 * 404 not found page
 * Related Requirement:
 */
import { Component, OnDestroy, OnInit } from '@angular/core';
import { interval } from 'rxjs';

@Component({
  selector: 'app-hourglass',
  standalone: true,
  imports: [],
  templateUrl: './hourglass.component.html',
  styleUrl: './hourglass.component.scss'
})
export class HourglassComponent {
  protected isFlipped=false;
  protected isFlowing=true;

  private intervalID?:number;

  ngOnInit():void{
    this.startAnimation();
  }

  ngOnDestroy():void{
    if (this.intervalID){
      window.clearInterval(this.intervalID);
    }
  }

  /*helper function*/
  private startAnimation():void{
    this.intervalID=window.setInterval(()=>{
      this.isFlowing= false;

      setTimeout(()=>{
        this.isFlipped=!this.isFlipped;
        this.isFlowing=true;
      },500);
    },5000);
  }
}
