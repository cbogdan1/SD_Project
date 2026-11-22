package ro.tuc.monitoring.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.monitoring.entities.HourlyConsumption;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HourlyConsumptionRepository extends JpaRepository<HourlyConsumption, Long> {

    // the row of one device for one hour (empty if no measurement arrived yet for that hour)
    Optional<HourlyConsumption> findByDeviceIdAndHourStart(UUID deviceId, LocalDateTime hourStart);

    // the rows of one device between two hours, both included (used for one day: 00:00 ... 23:00)
    List<HourlyConsumption> findByDeviceIdAndHourStartBetween(UUID deviceId, LocalDateTime from, LocalDateTime to);
}
