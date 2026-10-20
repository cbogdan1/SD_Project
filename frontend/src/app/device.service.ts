import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Device {
  id?: string;
  description: string;
  address: string;
  maxEnergyConsumption: number;
  userId: string | null; // owner; null = unassigned
}

// Relative URL: same origin as the page, Traefik routes /devices to device-service and strips the prefix.
const API = '/devices/device';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  private http = inject(HttpClient);

  getAll() { return this.http.get<Device[]>(API); }
  getByUser(userId: string) { return this.http.get<Device[]>(`${API}/user/${userId}`); }
  create(device: Device) { return this.http.post<string>(API, device); }
  // also used to assign/unassign: set userId and save
  update(id: string, device: Device) { return this.http.put<Device>(`${API}/${id}`, device); }
  delete(id: string) { return this.http.delete<void>(`${API}/${id}`); }
}
