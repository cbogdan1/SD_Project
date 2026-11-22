package ro.tuc.devices.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.devices.dtos.DeviceDTO;
import ro.tuc.devices.dtos.builders.DeviceBuilder;
import ro.tuc.devices.entities.AppUser;
import ro.tuc.devices.entities.Device;
import ro.tuc.devices.kafka.SyncEventPublisher;
import ro.tuc.devices.repositories.AppUserRepository;
import ro.tuc.devices.repositories.DeviceRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;
    private final AppUserRepository appUserRepository;
    private final SyncEventPublisher syncEventPublisher;

    public DeviceService(DeviceRepository deviceRepository, AppUserRepository appUserRepository,
                         SyncEventPublisher syncEventPublisher) {
        this.deviceRepository = deviceRepository;
        this.appUserRepository = appUserRepository;
        this.syncEventPublisher = syncEventPublisher;
    }

    public List<DeviceDTO> findDevices() {
        return deviceRepository.findAll().stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDTO findDeviceById(UUID id) {
        return DeviceBuilder.toDeviceDTO(getOrThrow(id));
    }

    // In insert, update and delete: if Kafka is down, publish() throws and @Transactional rolls back the change,
    // so device-service and monitoring-service never disagree about a device.

    /** Saves the device, then publishes DEVICE_CREATED (monitoring-service adds it to its own device table). */
    @Transactional
    public UUID insert(DeviceDTO dto) {
        checkUserExists(dto.getUserId());
        Device device = deviceRepository.save(DeviceBuilder.toEntity(dto));
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        syncEventPublisher.publish(new SyncEvent(SyncEventType.DEVICE_CREATED, device.getId(), device.getUserId()));
        return device.getId();
    }

    /** Also used to assign/unassign a device. DEVICE_UPDATED tells monitoring-service the (new) owner. */
    @Transactional
    public DeviceDTO update(UUID id, DeviceDTO dto) {
        Device device = getOrThrow(id);
        checkUserExists(dto.getUserId());
        device.setName(dto.getName());
        device.setDescription(dto.getDescription());
        device.setAddress(dto.getAddress());
        device.setMaxEnergyConsumption(dto.getMaxEnergyConsumption());
        device.setUserId(dto.getUserId());
        Device saved = deviceRepository.save(device);
        syncEventPublisher.publish(new SyncEvent(SyncEventType.DEVICE_UPDATED, id, saved.getUserId()));
        return DeviceBuilder.toDeviceDTO(saved);
    }

    /** Deletes the device, then publishes DEVICE_DELETED (monitoring-service deletes it and its consumption). */
    @Transactional
    public void delete(UUID id) {
        deviceRepository.delete(getOrThrow(id));
        syncEventPublisher.publish(new SyncEvent(SyncEventType.DEVICE_DELETED, id, null));
    }

    public List<DeviceDTO> findDevicesByUser(UUID userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    /** USER_CREATED event (sent by user-service): saves the user id in the local app_user table. */
    public void addUser(UUID userId) {
        appUserRepository.save(new AppUser(userId));
        LOGGER.info("User {} was added to app_user", userId);
    }

    /** USER_DELETED event: deletes the user id. The FK (ON DELETE SET NULL) unassigns the user's devices. */
    @Transactional
    public void deleteUser(UUID userId) {
        int count = appUserRepository.deleteDirect(userId);
        LOGGER.info("Deleted {} row(s) from app_user for user {}", count, userId);
    }

    // A device can only be assigned to a user that exists in the local app_user table (404 otherwise)
    private void checkUserExists(UUID userId) {
        if (userId != null && !appUserRepository.existsById(userId)) {
            LOGGER.error("User with id {} was not found in app_user", userId);
            throw new ResourceNotFoundException("User with id: " + userId);
        }
    }

    private Device getOrThrow(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Device with id {} was not found in db", id);
            return new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        });
    }
}
