package ro.tuc.devices.dtos.builders;

import ro.tuc.devices.dtos.DeviceDTO;
import ro.tuc.devices.entities.Device;

public class DeviceBuilder {

    private DeviceBuilder() {
    }

    public static DeviceDTO toDeviceDTO(Device device) {
        return new DeviceDTO(device.getId(), device.getDescription(), device.getAddress(), device.getMaxEnergyConsumption());
    }

    public static Device toEntity(DeviceDTO dto) {
        return new Device(dto.getDescription(), dto.getAddress(), dto.getMaxEnergyConsumption());
    }
}
