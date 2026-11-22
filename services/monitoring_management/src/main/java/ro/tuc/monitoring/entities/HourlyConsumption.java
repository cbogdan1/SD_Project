package ro.tuc.monitoring.entities;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/** The total energy (kWh) used by one device in one hour. There is one row per device per hour. */
@Entity
@Table(name = "hourly_consumption")
public class HourlyConsumption implements Serializable {

    private static final long serialVersionUID = 1L;

    // BIGSERIAL in the database: PostgreSQL generates the number
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Type(type = "uuid-binary")
    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    // start of the hour: the row for 18:00 holds the sum of the measurements from 18:00 to 18:50
    @Column(name = "hour_start", nullable = false)
    private LocalDateTime hourStart;

    @Column(name = "energy_kwh", nullable = false)
    private double energyKwh;

    public HourlyConsumption() {
    }

    public HourlyConsumption(UUID deviceId, LocalDateTime hourStart, double energyKwh) {
        this.deviceId = deviceId;
        this.hourStart = hourStart;
        this.energyKwh = energyKwh;
    }

    public Long getId() {
        return id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public LocalDateTime getHourStart() {
        return hourStart;
    }

    public double getEnergyKwh() {
        return energyKwh;
    }

    public void setEnergyKwh(double energyKwh) {
        this.energyKwh = energyKwh;
    }
}
