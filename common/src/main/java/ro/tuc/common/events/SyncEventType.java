package ro.tuc.common.events;

/** What happened. USER_CREATED comes from auth-service, USER_DELETED from user-service, DEVICE_* from device-service. */
public enum SyncEventType {
    USER_CREATED,
    USER_DELETED,
    DEVICE_CREATED,
    DEVICE_UPDATED,
    DEVICE_DELETED
}
