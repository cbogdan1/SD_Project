package ro.tuc.monitoring.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One message from the device simulator, for example:
 * {"timestamp":"2026-11-22T18:40:00","device_id":"d1eebc99-...","measurement_value":0.1234}
 */
public class MeasurementDTO {

    // local time at the start of the 10-minute interval
    private LocalDateTime timestamp;

    @JsonProperty("device_id")
    private UUID deviceId;

    // energy used in those 10 minutes, in kWh
    @JsonProperty("measurement_value")
    private double measurementValue;

    public MeasurementDTO() {
    }

    public MeasurementDTO(LocalDateTime timestamp, UUID deviceId, double measurementValue) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.measurementValue = measurementValue;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public double getMeasurementValue() {
        return measurementValue;
    }

    public void setMeasurementValue(double measurementValue) {
        this.measurementValue = measurementValue;
    }
}
