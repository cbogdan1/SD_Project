import { Component, inject, signal } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { Device, DeviceService } from '../device.service';
import { errorText } from '../http-error';

@Component({
  selector: 'app-my-devices',
  template: `
    <h2>My devices</h2>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    <table>
      <thead><tr><th>Description</th><th>Address</th><th>Max consumption</th></tr></thead>
      <tbody>
        @for (d of devices(); track d.id) {
          <tr><td>{{ d.description }}</td><td>{{ d.address }}</td><td>{{ d.maxEnergyConsumption }}</td></tr>
        } @empty { <tr><td colspan="3">No devices assigned to you yet</td></tr> }
      </tbody>
    </table>
  `,
})
export class MyDevicesComponent {
  devices = signal<Device[]>([]);
  error = signal('');

  constructor() {
    // the gateway only lets a CLIENT read the devices of the userId inside their own token
    inject(DeviceService).getByUser(inject(AuthService).userId()!)
      .subscribe({ next: d => this.devices.set(d), error: e => this.error.set(errorText(e)) });
  }
}
