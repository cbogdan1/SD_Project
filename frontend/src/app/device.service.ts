import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface Device { id: string; description: string; address: string; maxEnergyConsumption: number; }

// Relative URL: same origin as the page, Traefik routes /devices to device-service and strips the prefix.
const API = '/devices/device';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  private http = inject(HttpClient);

  getAll() { return this.http.get<Device[]>(API); }
  getById(id: string) { return this.http.get<Device>(`${API}/${id}`); }
  create(device: Omit<Device, 'id'>) { return this.http.post<string>(API, device); }
  update(id: string, device: Omit<Device, 'id'>) { return this.http.put<Device>(`${API}/${id}`, device); }
  delete(id: string) { return this.http.delete<void>(`${API}/${id}`); }
}
