import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TimerService } from '../../../core/services/timer.service';

@Component({
  selector: 'app-floating-timer',
  standalone: true,
  imports: [],
  templateUrl: './floating-timer.component.html',
  styleUrl: './floating-timer.component.scss'
})
export class FloatingTimerComponent implements OnInit{
  readonly timerService= inject(TimerService);
  private readonly router= inject(Router);

  ngOnInit(): void{
    this.loadActiveTimer();
  }

  private loadActiveTimer(): void{
    this.timerService.getActiveTimer().subscribe({
      next:(response)=>{
        if(response?.active){
          this.timerService.setActiveTimer(response);
        }else{
          this.timerService.clearActiveTimer();
        }
      },

      error:(error)=>{
        if(error.status!== 204 && error.status!==404){
          console.error('[FloatingTimer] Failed to load active timer:', error);
        }
      },
    });
  }

  openTimer(): void{
    this.router.navigate(['/log-time']);
    this.timerService.requestOpenTimerPanel();
  }

  formatElapsed(totalSeconds: number): string{
    const hours= Math.floor(totalSeconds/3600);
    const minutes= Math.floor((totalSeconds%3600)/60,);

    const seconds=totalSeconds%60;
    return [hours, minutes, seconds].map(
      (value)=> value.toString().padStart(2, '0')
    ).join(':');
  }
}
