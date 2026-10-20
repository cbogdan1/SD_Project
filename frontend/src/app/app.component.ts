import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.role(); as role) {
      <nav>
        @if (role === 'ADMIN') {
          <a routerLink="/admin/users" routerLinkActive="active">Users</a>
          <a routerLink="/admin/devices" routerLinkActive="active">Devices</a>
        } @else {
          <a routerLink="/my-devices" routerLinkActive="active">My devices</a>
        }
        <button type="button" (click)="auth.logout()">Log out</button>
      </nav>
    }
    <router-outlet />
  `,
})
export class AppComponent {
  auth = inject(AuthService);
}
