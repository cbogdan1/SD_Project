import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav>
      <a routerLink="/people">Users</a>
      <a routerLink="/device-list">Devices</a>
    </nav>
    <router-outlet />
  `,
})
export class AppComponent {}
