package ro.tuc.devices.entities;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-binary")
    private UUID id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "max_energy_consumption", nullable = false)
    private double maxEnergyConsumption;

    // owner's Person id from user-service; null = unassigned (no FK, it lives in another database)
    @Type(type = "uuid-binary")
    @Column(name = "user_id")
    private UUID userId;

    public Device() {
    }

    public Device(String description, String address, double maxEnergyConsumption, UUID userId) {
        this.description = description;
        this.address = address;
        this.maxEnergyConsumption = maxEnergyConsumption;
        this.userId = userId;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
