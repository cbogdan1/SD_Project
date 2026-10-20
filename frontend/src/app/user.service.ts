import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Person { id: string; name: string; age: number; }
export interface PersonDetails { id?: string; name: string; address: string; age: number; }

// Relative URL: same origin as the page, Traefik routes /users to user-service and strips the prefix.
const API = '/users/person';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  getAll() { return this.http.get<Person[]>(API); }
  getById(id: string) { return this.http.get<PersonDetails>(`${API}/${id}`); }
  update(id: string, person: PersonDetails) { return this.http.put<PersonDetails>(`${API}/${id}`, person); }
  // removes the login and unassigns the devices too (done by user-service)
  delete(id: string) { return this.http.delete<void>(`${API}/${id}`); }
  // creating a user goes through AuthService.register, so the account also gets a login
}
