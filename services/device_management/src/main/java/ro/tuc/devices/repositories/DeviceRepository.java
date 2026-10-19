package ro.tuc.devices.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.tuc.devices.entities.Device;

import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
}
