import { Component, OnInit } from '@angular/core';
import { broadcastResponseToMainFrame } from '@azure/msal-browser/redirect-bridge';

@Component({
  selector: 'app-microsoft-redirect',
  standalone: true,
  template: '<p>Processing Microsoft sign in...</p>',
})
export class MicrosoftRedirectComponent implements OnInit {

  ngOnInit(): void {
    broadcastResponseToMainFrame().catch((error: Error) => {
      console.error('[Microsoft Auth] Failed to broadcast authentication response:', error, );
    });
  }
}