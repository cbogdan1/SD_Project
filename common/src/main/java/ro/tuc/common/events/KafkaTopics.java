package ro.tuc.common.events;

/** Names of the Kafka topics used by the services. */
public final class KafkaTopics {

    // data collection: device simulator -> monitoring-service
    public static final String DEVICE_MEASUREMENTS = "device-measurements";

    // synchronization: auth-service, user-service and device-service -> every service that keeps a copy of users/devices
    public static final String SYNC_EVENTS = "sync-events";

    private KafkaTopics() {
    }
}
