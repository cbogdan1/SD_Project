package ro.tuc.monitoring.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.common.handlers.exceptions.model.ForbiddenException;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.monitoring.dtos.HourlyConsumptionDTO;
import ro.tuc.monitoring.dtos.MeasurementDTO;
import ro.tuc.monitoring.entities.Device;
import ro.tuc.monitoring.entities.HourlyConsumption;
import ro.tuc.monitoring.repositories.DeviceRepository;
import ro.tuc.monitoring.repositories.HourlyConsumptionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConsumptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumptionService.class);
    private static final int HOURS_PER_DAY = 24;

    private final DeviceRepository deviceRepository;
    private final HourlyConsumptionRepository hourlyConsumptionRepository;

    public ConsumptionService(DeviceRepository deviceRepository,
                              HourlyConsumptionRepository hourlyConsumptionRepository) {
        this.deviceRepository = deviceRepository;
        this.hourlyConsumptionRepository = hourlyConsumptionRepository;
    }

    /**
     * Adds one 10-minute measurement to the total of its hour (one row per device per hour).
     * Measurements of unknown devices (no DEVICE_CREATED event received) are ignored.
     */
    @Transactional
    public void addMeasurement(MeasurementDTO measurement) {
        UUID deviceId = measurement.getDeviceId();
        if (!deviceRepository.existsById(deviceId)) {
            LOGGER.warn("Unknown device {}, measurement ignored", deviceId);
            return;
        }
        // 18:40 -> 18:00, so all the measurements of one hour go to the same row
        LocalDateTime hourStart = measurement.getTimestamp().truncatedTo(ChronoUnit.HOURS);
        // first measurement of this hour: start a new row with 0 kWh
        HourlyConsumption hour = hourlyConsumptionRepository.findByDeviceIdAndHourStart(deviceId, hourStart)
                .orElse(new HourlyConsumption(deviceId, hourStart, 0));
        hour.setEnergyKwh(hour.getEnergyKwh() + measurement.getMeasurementValue());
        hourlyConsumptionRepository.save(hour);
        LOGGER.info("Device {}, hour {}: total is now {} kWh", deviceId, hourStart, roundKwh(hour.getEnergyKwh()));
    }

    /**
     * Returns the 24 hourly totals of one device for one day (0 for the hours without data).
     * The gateway already checked that a CLIENT asks only with his own userId; here we check that the device is his.
     */
    public List<HourlyConsumptionDTO> getDailyConsumption(UUID userId, UUID deviceId, LocalDate date) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(() -> {
            LOGGER.error("Device with id {} was not found in db", deviceId);
            return new ResourceNotFoundException("Device with id: " + deviceId);
        });
        if (!userId.equals(device.getUserId())) {
            LOGGER.error("Device {} does not belong to user {}", deviceId, userId);
            throw new ForbiddenException("Device with id: " + deviceId);
        }

        LocalDateTime firstHour = date.atStartOfDay();                    // 00:00
        LocalDateTime lastHour = firstHour.plusHours(HOURS_PER_DAY - 1);  // 23:00
        double[] energyPerHour = new double[HOURS_PER_DAY];
        for (HourlyConsumption row : hourlyConsumptionRepository.findByDeviceIdAndHourStartBetween(
                deviceId, firstHour, lastHour)) {
            energyPerHour[row.getHourStart().getHour()] = row.getEnergyKwh();
        }

        List<HourlyConsumptionDTO> result = new ArrayList<>();
        for (int hour = 0; hour < HOURS_PER_DAY; hour++) {
            result.add(new HourlyConsumptionDTO(hour, roundKwh(energyPerHour[hour])));
        }
        return result;
    }

    // 3 decimals are enough for kWh (and hide sums like 0.30000000000000004)
    private static double roundKwh(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
