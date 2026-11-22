package ro.tuc.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * One message on the "sync-events" topic. It travels as JSON, for example:
 * {"type":"DEVICE_CREATED","id":"<device id>","userId":"<owner id>"}
 * {"type":"USER_CREATED","id":"<user id>","username":"ion","name":"Ion Popescu","address":"...","age":22}
 */
// null fields are left out of the JSON, so each event type carries only its own fields
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncEvent {

    private SyncEventType type;

    // the user id for USER_* events, the device id for DEVICE_* events
    private UUID id;

    // only for DEVICE_* events: the owner of the device (null = unassigned)
    private UUID userId;

    // only for USER_CREATED (sent by auth-service): the profile, so user-service can create the person
    private String username;
    private String name;
    private String address;
    private Integer age;

    // Jackson needs an empty constructor to create the object from JSON
    public SyncEvent() {
    }

    public SyncEvent(SyncEventType type, UUID id, UUID userId) {
        this.type = type;
        this.id = id;
        this.userId = userId;
    }

    public SyncEventType getType() {
        return type;
    }

    public void setType(SyncEventType type) {
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
