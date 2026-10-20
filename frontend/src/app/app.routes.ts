import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from './auth/auth.service';
import { roleGuard } from './auth/role.guard';
import { LoginComponent } from './pages/login.component';
import { AdminUsersComponent } from './pages/admin-users.component';
import { AdminDevicesComponent } from './pages/admin-devices.component';
import { MyDevicesComponent } from './pages/my-devices.component';

// Paths must not start with /users, /devices or /auth: Traefik sends those prefixes to the backends, so a refresh would miss the SPA.
export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'admin/users', component: AdminUsersComponent, canActivate: [roleGuard('ADMIN')] },
  { path: 'admin/devices', component: AdminDevicesComponent, canActivate: [roleGuard('ADMIN')] },
  { path: 'my-devices', component: MyDevicesComponent, canActivate: [roleGuard('CLIENT')] },
  { path: '**', redirectTo: () => inject(AuthService).homeUrl() },
];
