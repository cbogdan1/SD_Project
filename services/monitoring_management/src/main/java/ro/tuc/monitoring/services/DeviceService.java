package ro.tuc.monitoring.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.monitoring.entities.Device;
import ro.tuc.monitoring.repositories.DeviceRepository;

import java.util.UUID;

/** Keeps the local device table (id + owner) the same as in device-service. Called by the sync event listener. */
@Service
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /** DEVICE_CREATED and DEVICE_UPDATED: inserts the device, or overwrites its owner if it already exists. */
    public void saveDevice(UUID deviceId, UUID userId) {
        deviceRepository.save(new Device(deviceId, userId));
        LOGGER.info("Device {} saved with owner {}", deviceId, userId);
    }

    /** DEVICE_DELETED: deletes the device and (ON DELETE CASCADE) its hourly consumption. */
    @Transactional
    public void deleteDevice(UUID deviceId) {
        int count = deviceRepository.deleteDirect(deviceId);
        LOGGER.info("Deleted {} row(s) from device for device {}", count, deviceId);
    }

    /** USER_DELETED: the devices of the deleted user become unassigned. */
    @Transactional
    public void unassignUser(UUID userId) {
        int count = deviceRepository.unassignUser(userId);
        LOGGER.info("Unassigned {} device(s) of the deleted user {}", count, userId);
    }
}
