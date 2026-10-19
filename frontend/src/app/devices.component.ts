import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { DeviceService } from './device.service';

@Component({
  selector: 'app-devices',
  template: `
    <h2>Devices</h2>
    <ul>
      @for (d of devices(); track d.id) {
        <li>{{ d.description }} – {{ d.address }} (max {{ d.maxEnergyConsumption }})</li>
      } @empty {
        <li>No devices (or device-service unavailable)</li>
      }
    </ul>
  `,
})
export class DevicesComponent {
  devices = toSignal(inject(DeviceService).getAll().pipe(catchError(() => of([]))), { initialValue: [] });
}
