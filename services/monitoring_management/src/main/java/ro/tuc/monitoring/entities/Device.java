package ro.tuc.monitoring.entities;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/**
 * Local copy of a device from device-service: only its id and its owner.
 * Filled and updated by the DEVICE_* sync events, so monitoring-service knows which devices exist and whose they are.
 */
@Entity
@Table(name = "device")
public class Device implements Serializable {

    private static final long serialVersionUID = 1L;

    // same id as the device in device-service, so it is not generated here
    @Id
    @Type(type = "uuid-binary")
    private UUID id;

    // the owner's user id; null = unassigned
    @Type(type = "uuid-binary")
    @Column(name = "user_id")
    private UUID userId;

    public Device() {
    }

    public Device(UUID id, UUID userId) {
        this.id = id;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
