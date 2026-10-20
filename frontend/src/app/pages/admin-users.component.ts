import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { AuthService, RegisterRequest } from '../auth/auth.service';
import { Person, UserService } from '../user.service';
import { errorText } from '../http-error';

const EMPTY: RegisterRequest = { username: '', password: '', role: 'CLIENT', name: '', address: '', age: 18 };

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule],
  template: `
    <h2>Users</h2>
    <table>
      <thead><tr><th>Name</th><th>Age</th><th></th></tr></thead>
      <tbody>
        @for (u of users(); track u.id) {
          <tr>
            <td>{{ u.name }}</td><td>{{ u.age }}</td>
            <td>
              <button type="button" (click)="edit(u)">Edit</button>
              <button type="button" (click)="remove(u)">Delete</button>
            </td>
          </tr>
        } @empty { <tr><td colspan="3">No users</td></tr> }
      </tbody>
    </table>

    <h3>{{ editingId() ? 'Edit user' : 'New user' }}</h3>
    <form (ngSubmit)="save()" class="stack">
      @if (!editingId()) {
        <label>Username <input name="username" [(ngModel)]="form.username" required></label>
        <label>Password <input name="password" type="password" [(ngModel)]="form.password" required></label>
        <label>Role
          <select name="role" [(ngModel)]="form.role">
            <option value="CLIENT">CLIENT</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>
      }
      <label>Name <input name="name" [(ngModel)]="form.name" required></label>
      <label>Address <input name="address" [(ngModel)]="form.address" required></label>
      <label>Age <input name="age" type="number" [(ngModel)]="form.age" required></label>
      <div>
        <button type="submit">Save</button>
        @if (editingId()) { <button type="button" (click)="reset()">Cancel</button> }
      </div>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </form>
  `,
})
export class AdminUsersComponent {
  private userService = inject(UserService);
  private auth = inject(AuthService);

  users = signal<Person[]>([]);
  editingId = signal<string | null>(null);
  error = signal('');
  form: RegisterRequest = { ...EMPTY };

  constructor() {
    this.load();
  }

  load() {
    this.userService.getAll().subscribe({ next: u => this.users.set(u), error: e => this.error.set(errorText(e)) });
  }

  edit(user: Person) {
    this.userService.getById(user.id).subscribe(details => {
      this.form = { ...EMPTY, ...details };
      this.editingId.set(user.id);
    });
  }

  save() {
    const id = this.editingId();
    const { name, address, age } = this.form;
    // new users go through auth-service so they also get a login; the admin token allows role ADMIN
    const request: Observable<unknown> = id
      ? this.userService.update(id, { name, address, age })
      : this.auth.register(this.form);
    this.run(request);
  }

  remove(user: Person) {
    if (confirm(`Delete ${user.name}? Their login is removed and their devices become unassigned.`)) {
      this.run(this.userService.delete(user.id));
    }
  }

  reset() {
    this.form = { ...EMPTY };
    this.editingId.set(null);
  }

  private run(request: Observable<unknown>) {
    this.error.set('');
    request.subscribe({ next: () => { this.reset(); this.load(); }, error: e => this.error.set(errorText(e)) });
  }
}
