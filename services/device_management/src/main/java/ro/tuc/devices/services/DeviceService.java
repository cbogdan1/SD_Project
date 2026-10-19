package ro.tuc.devices.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.tuc.common.handlers.exceptions.model.ResourceNotFoundException;
import ro.tuc.devices.dtos.DeviceDTO;
import ro.tuc.devices.dtos.builders.DeviceBuilder;
import ro.tuc.devices.entities.Device;
import ro.tuc.devices.repositories.DeviceRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
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
        Device device = deviceRepository.save(DeviceBuilder.toEntity(dto));
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        return device.getId();
    }

    public DeviceDTO update(UUID id, DeviceDTO dto) {
        Device device = getOrThrow(id);
        device.setDescription(dto.getDescription());
        device.setAddress(dto.getAddress());
        device.setMaxEnergyConsumption(dto.getMaxEnergyConsumption());
        return DeviceBuilder.toDeviceDTO(deviceRepository.save(device));
    }

    public void delete(UUID id) {
        deviceRepository.delete(getOrThrow(id));
    }

    private Device getOrThrow(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> {
            LOGGER.error("Device with id {} was not found in db", id);
            return new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        });
    }
}
