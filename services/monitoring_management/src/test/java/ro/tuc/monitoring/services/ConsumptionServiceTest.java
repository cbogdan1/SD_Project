package ro.tuc.monitoring.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ro.tuc.common.handlers.exceptions.model.ForbiddenException;
import ro.tuc.monitoring.dtos.HourlyConsumptionDTO;
import ro.tuc.monitoring.dtos.MeasurementDTO;
import ro.tuc.monitoring.entities.Device;
import ro.tuc.monitoring.entities.HourlyConsumption;
import ro.tuc.monitoring.repositories.DeviceRepository;
import ro.tuc.monitoring.repositories.HourlyConsumptionRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test with mocked repositories: no database and no Kafka needed
class ConsumptionServiceTest {

    private final UUID owner = UUID.randomUUID();
    private final UUID deviceId = UUID.randomUUID();
    private final LocalDate day = LocalDate.of(2026, 11, 22);

    private DeviceRepository deviceRepository;
    private HourlyConsumptionRepository hourlyRepository;
    private ConsumptionService service;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepository.class);
        hourlyRepository = mock(HourlyConsumptionRepository.class);
        service = new ConsumptionService(deviceRepository, hourlyRepository);
    }

    @Test
    void measurementIsAddedToTheTotalOfItsHour() {
        HourlyConsumption hour18 = new HourlyConsumption(deviceId, day.atTime(18, 0), 0.5);
        when(deviceRepository.existsById(deviceId)).thenReturn(true);
        when(hourlyRepository.findByDeviceIdAndHourStart(deviceId, day.atTime(18, 0))).thenReturn(Optional.of(hour18));

        // a measurement at 18:40 belongs to the 18:00 row
        service.addMeasurement(new MeasurementDTO(day.atTime(18, 40), deviceId, 0.25));

        assertEquals(0.75, hour18.getEnergyKwh(), 1e-9);
        verify(hourlyRepository).save(hour18);
    }

    @Test
    void measurementOfUnknownDeviceIsIgnored() {
        when(deviceRepository.existsById(deviceId)).thenReturn(false);

        service.addMeasurement(new MeasurementDTO(LocalDateTime.now(), deviceId, 0.25));

        verify(hourlyRepository, never()).save(any());
    }

    @Test
    void dailyConsumptionHas24HoursWithZeroWhenThereIsNoData() {
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(new Device(deviceId, owner)));
        when(hourlyRepository.findByDeviceIdAndHourStartBetween(deviceId, day.atStartOfDay(), day.atTime(23, 0)))
                .thenReturn(List.of(new HourlyConsumption(deviceId, day.atTime(18, 0), 1.2)));

        List<HourlyConsumptionDTO> result = service.getDailyConsumption(owner, deviceId, day);

        assertEquals(24, result.size());
        assertEquals(1.2, result.get(18).getEnergyKwh(), 1e-9);
        assertEquals(0.0, result.get(3).getEnergyKwh(), 1e-9);
    }

    @Test
    void deviceOfAnotherUserIsForbidden() {
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(new Device(deviceId, owner)));

        assertThrows(ForbiddenException.class,
                () -> service.getDailyConsumption(UUID.randomUUID(), deviceId, day));
    }
}
