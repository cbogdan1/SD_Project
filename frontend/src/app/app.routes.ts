import { Routes } from '@angular/router';
import { UsersComponent } from './users.component';
import { DevicesComponent } from './devices.component';

// Paths must not start with /users or /devices: Traefik sends those prefixes to the backends, so a refresh would miss the SPA.
export const routes: Routes = [
  { path: 'people', component: UsersComponent },   // pe pathul /people se incarca componeneta UserComponent
  { path: 'device-list', component: DevicesComponent },
  { path: '**', redirectTo: 'people' },
];
