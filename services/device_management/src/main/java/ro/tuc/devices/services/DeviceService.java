package ro.tuc.devices.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.devices.dtos.DeviceDTO;
import ro.tuc.devices.dtos.builders.DeviceBuilder;
import ro.tuc.devices.entities.AppUser;
import ro.tuc.devices.entities.Device;
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

    public DeviceService(DeviceRepository deviceRepository, AppUserRepository appUserRepository) {
        this.deviceRepository = deviceRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<DeviceDTO> findDevices() {
        return deviceRepository.findAll().stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDTO findDeviceById(UUID id) {
        return DeviceBuilder.toDeviceDTO(getOrThrow(id));
    }

    public UUID insert(DeviceDTO dto) {
        checkUserExists(dto.getUserId());
        Device device = deviceRepository.save(DeviceBuilder.toEntity(dto));
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        return device.getId();
    }

    public DeviceDTO update(UUID id, DeviceDTO dto) {
        Device device = getOrThrow(id);
        checkUserExists(dto.getUserId());
        device.setName(dto.getName());
        device.setDescription(dto.getDescription());
        device.setAddress(dto.getAddress());
        device.setMaxEnergyConsumption(dto.getMaxEnergyConsumption());
        device.setUserId(dto.getUserId());
        return DeviceBuilder.toDeviceDTO(deviceRepository.save(device));
    }

    public void delete(UUID id) {
        deviceRepository.delete(getOrThrow(id));
    }

    public List<DeviceDTO> findDevicesByUser(UUID userId) {
        return deviceRepository.findByUserId(userId).stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    /** Called by user-service after a user is created: saves the user id in the local app_user table. */
    public void addUser(UUID userId) {
        appUserRepository.save(new AppUser(userId));
        LOGGER.debug("User {} was added to app_user", userId);
    }

    /** Called by user-service when a user is deleted. The FK (ON DELETE SET NULL) unassigns the user's devices. */
    @Transactional
    public void deleteUser(UUID userId) {
        int count = appUserRepository.deleteDirect(userId);
        LOGGER.debug("Deleted {} row(s) from app_user for user {}", count, userId);
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
