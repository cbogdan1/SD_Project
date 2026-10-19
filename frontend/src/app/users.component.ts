import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { UserService } from './user.service';

@Component({
  selector: 'app-users',
  template: `
    <h2>Users</h2>
    <ul>
      @for (p of persons(); track p.id) {
        <li>{{ p.name }} ({{ p.age }})</li>
      } @empty {
        <li>No users (or user-service unavailable)</li>
      }
    </ul>
  `,
})
export class UsersComponent {
  persons = toSignal(inject(UserService).getAll().pipe(catchError(() => of([]))), { initialValue: [] });
}
