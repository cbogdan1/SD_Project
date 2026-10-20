import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { AuthService, RegisterRequest } from '../auth/auth.service';
import { errorText } from '../http-error';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  template: `
    <h2>{{ registering() ? 'Register' : 'Login' }}</h2>
    <form (ngSubmit)="submit()" class="stack">
      <label>Username <input name="username" [(ngModel)]="form.username" required></label>
      <label>Password <input name="password" type="password" [(ngModel)]="form.password" required></label>
      @if (registering()) {
        <label>Name <input name="name" [(ngModel)]="form.name" required></label>
        <label>Address <input name="address" [(ngModel)]="form.address" required></label>
        <label>Age <input name="age" type="number" [(ngModel)]="form.age" required></label>
      }
      <button type="submit">{{ registering() ? 'Create account' : 'Log in' }}</button>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </form>
    <button type="button" class="link" (click)="registering.set(!registering()); error.set('')">
      {{ registering() ? 'Already have an account? Log in' : 'No account? Register' }}
    </button>
  `,
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  registering = signal(false);
  error = signal('');
  form: RegisterRequest = { username: '', password: '', name: '', address: '', age: 18 };

  constructor() {
    if (this.auth.role()) this.router.navigateByUrl(this.auth.homeUrl());
  }

  submit() {
    const { username, password } = this.form;
    // public registration always creates a CLIENT; log in right after
    const request = this.registering()
      ? this.auth.register(this.form).pipe(switchMap(() => this.auth.login(username, password)))
      : this.auth.login(username, password);
    request.subscribe({
      next: () => this.router.navigateByUrl(this.auth.homeUrl()),
      error: err => this.error.set(err.status === 401 ? 'Wrong username or password' : errorText(err)),
    });
  }
}
