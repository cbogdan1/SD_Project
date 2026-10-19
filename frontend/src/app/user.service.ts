import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Person { id: string; name: string; age: number; }

// Relative URL: same origin as the page, Traefik routes /users to user-service and strips the prefix.
const API = '/users/person';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  getAll() { return this.http.get<Person[]>(API); }
  getById(id: string) { return this.http.get<Person>(`${API}/${id}`); }
  create(person: Omit<Person, 'id'>) { return this.http.post<string>(API, person); }
}
