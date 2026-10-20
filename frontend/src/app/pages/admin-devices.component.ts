import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { Device, DeviceService } from '../device.service';
import { Person, UserService } from '../user.service';
import { errorText } from '../http-error';

const EMPTY: Device = { description: '', address: '', maxEnergyConsumption: 0, userId: null };

@Component({
  selector: 'app-admin-devices',
  imports: [FormsModule],
  template: `
    <h2>Devices</h2>
    <table>
      <thead><tr><th>Description</th><th>Address</th><th>Max consumption</th><th>Owner</th><th></th></tr></thead>
      <tbody>
        @for (d of devices(); track d.id) {
          <tr>
            <td>{{ d.description }}</td><td>{{ d.address }}</td><td>{{ d.maxEnergyConsumption }}</td>
            <td>
              <select [attr.aria-label]="'Owner of ' + d.description" [ngModel]="d.userId" (ngModelChange)="assign(d, $event)">
                <option [ngValue]="null">— unassigned —</option>
                @for (u of users(); track u.id) { <option [ngValue]="u.id">{{ u.name }}</option> }
              </select>
            </td>
            <td>
              <button type="button" (click)="edit(d)">Edit</button>
              <button type="button" (click)="remove(d)">Delete</button>
            </td>
          </tr>
        } @empty { <tr><td colspan="5">No devices</td></tr> }
      </tbody>
    </table>

    <h3>{{ editingId() ? 'Edit device' : 'New device' }}</h3>
    <form (ngSubmit)="save()" class="stack">
      <label>Description <input name="description" [(ngModel)]="form.description" required></label>
      <label>Address <input name="address" [(ngModel)]="form.address" required></label>
      <label>Max energy consumption <input name="max" type="number" min="0" [(ngModel)]="form.maxEnergyConsumption" required></label>
      <label>Owner
        <select name="userId" [(ngModel)]="form.userId">
          <option [ngValue]="null">— unassigned —</option>
          @for (u of users(); track u.id) { <option [ngValue]="u.id">{{ u.name }}</option> }
        </select>
      </label>
      <div>
        <button type="submit">Save</button>
        @if (editingId()) { <button type="button" (click)="reset()">Cancel</button> }
      </div>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </form>
  `,
})
export class AdminDevicesComponent {
  private deviceService = inject(DeviceService);
  private userService = inject(UserService);

  devices = signal<Device[]>([]);
  users = signal<Person[]>([]);
  editingId = signal<string | null>(null);
  error = signal('');
  form: Device = { ...EMPTY };

  constructor() {
    this.load();
    this.userService.getAll().subscribe({ next: u => this.users.set(u), error: e => this.error.set(errorText(e)) });
  }

  load() {
    this.deviceService.getAll().subscribe({ next: d => this.devices.set(d), error: e => this.error.set(errorText(e)) });
  }

  assign(device: Device, userId: string | null) {
    this.run(this.deviceService.update(device.id!, { ...device, userId }));
  }

  edit(device: Device) {
    this.form = { ...device };
    this.editingId.set(device.id!);
  }

  save() {
    const id = this.editingId();
    this.run(id ? this.deviceService.update(id, this.form) : this.deviceService.create(this.form));
  }

  remove(device: Device) {
    if (confirm(`Delete ${device.description}?`)) {
      this.run(this.deviceService.delete(device.id!));
    }
  }

  reset() {
    this.form = { ...EMPTY };
    this.editingId.set(null);
  }

  private run(request: Observable<unknown>) {
    this.error.set('');
    // reload on error too, so a failed inline assign shows the real owner again
    request.subscribe({
      next: () => { this.reset(); this.load(); },
      error: e => { this.error.set(errorText(e)); this.load(); },
    });
  }
}
