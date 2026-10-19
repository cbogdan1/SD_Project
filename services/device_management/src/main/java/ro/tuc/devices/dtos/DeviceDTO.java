package ro.tuc.devices.dtos;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import java.util.UUID;

public class DeviceDTO {
    private UUID id;
    @NotBlank
    private String description;
    @NotBlank
    private String address;
    @PositiveOrZero
    private double maxEnergyConsumption;

    public DeviceDTO() {
    }

    public DeviceDTO(UUID id, String description, String address, double maxEnergyConsumption) {
        this.id = id;
        this.description = description;
        this.address = address;
        this.maxEnergyConsumption = maxEnergyConsumption;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getMaxEnergyConsumption() {
        return maxEnergyConsumption;
    }

    public void setMaxEnergyConsumption(double maxEnergyConsumption) {
        this.maxEnergyConsumption = maxEnergyConsumption;
    }
}
