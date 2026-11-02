package ro.tuc.devices.entities;

import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/** Local copy of a user id from user-service. device.user_id has a foreign key to this table. */
@Entity
@Table(name = "app_user")
public class AppUser implements Serializable {

    private static final long serialVersionUID = 1L;

    // same id as the Person in user-service, so it is not generated here
    @Id
    @Type(type = "uuid-binary")
    private UUID id;

    public AppUser() {
    }

    public AppUser(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
