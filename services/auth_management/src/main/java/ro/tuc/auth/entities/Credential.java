package ro.tuc.auth.entities;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.util.UUID;

@Entity
public class Credential implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-binary")
    private UUID id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // id of the Person in user-service (random for the seeded admin, who has no profile)
    @Type(type = "uuid-binary")
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    public Credential() {
    }

    public Credential(String username, String passwordHash, Role role, UUID userId) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UUID getUserId() {
        return userId;
    }
}
