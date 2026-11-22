import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import Chart from 'chart.js/auto';
import { AuthService } from '../auth/auth.service';
import { Device, DeviceService } from '../device.service';
import { MonitoringService } from '../monitoring.service';
import { errorText } from '../http-error';

@Component({
  selector: 'app-my-devices',
  imports: [FormsModule],
  template: `
    <h2>My devices</h2>
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    <table>
      <thead><tr><th>Name</th><th>Description</th><th>Address</th><th>Max consumption</th></tr></thead>
      <tbody>
        @for (d of devices(); track d.id) {
          <tr><td>{{ d.name }}</td><td>{{ d.description }}</td><td>{{ d.address }}</td><td>{{ d.maxEnergyConsumption }}</td></tr>
        } @empty { <tr><td colspan="4">No devices assigned to you yet</td></tr> }
      </tbody>
    </table>

    <h3>Consumption</h3>
    <form (ngSubmit)="showConsumption()">
      <label>Device
        <select name="deviceId" [(ngModel)]="deviceId">
          @for (d of devices(); track d.id) { <option [value]="d.id">{{ d.name }}</option> }
        </select>
      </label>
      <label>Day <input name="date" type="date" [(ngModel)]="date"></label>
      <button type="submit">Show</button>
    </form>
    <canvas #chart width="800" height="400"></canvas>
  `,
})
export class MyDevicesComponent {
  private auth = inject(AuthService);
  private monitoringService = inject(MonitoringService);
  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('chart');
  private chart?: Chart;

  devices = signal<Device[]>([]);
  error = signal('');
  deviceId = '';
  date = today();

  constructor() {
    // the gateway only lets a CLIENT read the devices of the userId inside their own token
    inject(DeviceService).getByUser(this.auth.userId()!).subscribe({
      next: d => {
        this.devices.set(d);
        this.deviceId = d[0]?.id ?? ''; // preselect the first device
      },
      error: e => this.error.set(errorText(e)),
    });
  }

  showConsumption() {
    if (!this.deviceId || !this.date) {
      return;
    }
    this.error.set('');
    this.monitoringService.getDailyConsumption(this.auth.userId()!, this.deviceId, this.date).subscribe({
      next: hours => this.drawChart(hours.map(h => h.energyKwh)),
      error: e => this.error.set(errorText(e)),
    });
  }

  // bar chart: OX = the hours 0..23, OY = the energy (kWh) used in that hour
  private drawChart(values: number[]) {
    this.chart?.destroy(); // a canvas holds only one chart, so remove the old one first
    this.chart = new Chart(this.canvas().nativeElement, {
      type: 'bar',
      data: {
        labels: values.map((_, hour) => hour),
        datasets: [{ label: `Energy per hour on ${this.date} (kWh)`, data: values }],
      },
      options: {
        responsive: false, // keep the 800 x 400 size of the canvas
        scales: {
          x: { title: { display: true, text: 'Hour' } },
          y: { title: { display: true, text: 'Energy [kWh]' }, beginAtZero: true },
        },
      },
    });
  }
}

// today's date as YYYY-MM-DD in local time (the format of <input type="date">)
function today(): string {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}
