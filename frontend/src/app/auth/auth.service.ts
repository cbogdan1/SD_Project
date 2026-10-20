import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';

export type Role = 'ADMIN' | 'CLIENT';

export interface RegisterRequest {
  username: string;
  password: string;
  role?: Role;
  name: string;
  address: string;
  age: number;
}

interface Session { token: string; role: Role; userId: string; exp: number; }

const STORAGE_KEY = 'auth_token';

// Relative URLs: Traefik exposes only /auth/login and /auth/register of auth-service.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private session = signal<Session | null>(restore());

  // An expired token counts as logged out; the gateway would reject it anyway.
  private active() {
    const s = this.session();
    return s && s.exp * 1000 > Date.now() ? s : null;
  }

  token = () => this.active()?.token ?? null;
  role = () => this.active()?.role ?? null;
  userId = () => this.active()?.userId ?? null;

  login(username: string, password: string) {
    return this.http.post<{ token: string }>('/auth/login', { username, password }).pipe(
      tap(({ token }) => {
        localStorage.setItem(STORAGE_KEY, token);
        this.session.set(decode(token));
      }),
    );
  }

  register(request: RegisterRequest) {
    return this.http.post<string>('/auth/register', request);
  }

  logout() {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    this.router.navigateByUrl('/login');
  }

  homeUrl() {
    switch (this.role()) {
      case 'ADMIN': return '/admin/users';
      case 'CLIENT': return '/my-devices';
      default: return '/login';
    }
  }
}

function restore(): Session | null {
  const token = localStorage.getItem(STORAGE_KEY);
  try {
    return token ? decode(token) : null;
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return null;
  }
}

// Reads the JWT payload for UI decisions only; the signature is verified server-side by auth-service.
function decode(token: string): Session {
  const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
  return { token, role: payload.role, userId: payload.userId, exp: payload.exp };
}
