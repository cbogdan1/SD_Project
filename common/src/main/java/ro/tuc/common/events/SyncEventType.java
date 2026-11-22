package ro.tuc.common.events;

/** What happened. USER_* events come from user-service, DEVICE_* events from device-service. */
public enum SyncEventType {
    USER_CREATED,
    USER_DELETED,
    DEVICE_CREATED,
    DEVICE_UPDATED,
    DEVICE_DELETED
}
