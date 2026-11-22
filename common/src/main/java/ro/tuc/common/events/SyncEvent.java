package ro.tuc.common.events;

import java.util.UUID;

/**
 * One message on the "sync-events" topic. It travels as JSON, for example:
 * {"type":"DEVICE_CREATED","id":"<device id>","userId":"<owner id>"}
 */
public class SyncEvent {

    private SyncEventType type;

    // the user id for USER_* events, the device id for DEVICE_* events
    private UUID id;

    // only for DEVICE_* events: the owner of the device (null = unassigned)
    private UUID userId;

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
}
