import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

// the energy used by a device in one hour of a day (one bar of the chart)
export interface HourlyConsumption {
  hour: number;
  energyKwh: number;
}

// Relative URL: same origin as the page, Traefik routes /monitoring to monitoring-service and strips the prefix.
const API = '/monitoring/consumption';

@Injectable({ providedIn: 'root' })
export class MonitoringService {
  private http = inject(HttpClient);

  // date = 'YYYY-MM-DD' (the value of <input type="date">); the answer always has 24 values (hours 0..23)
  getDailyConsumption(userId: string, deviceId: string, date: string) {
    return this.http.get<HourlyConsumption[]>(`${API}/user/${userId}`, { params: { deviceId, date } });
  }
}
