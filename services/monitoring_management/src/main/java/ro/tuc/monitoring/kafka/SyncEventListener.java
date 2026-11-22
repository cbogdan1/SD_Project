package ro.tuc.monitoring.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.monitoring.services.DeviceService;

/**
 * Reads the "sync-events" topic. monitoring-service keeps a copy of the devices and their owners,
 * so it reacts to the DEVICE_* events and to USER_DELETED (the owner disappears).
 */
@Component
public class SyncEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventListener.class);

    private final DeviceService deviceService;
    private final ObjectMapper objectMapper;

    public SyncEventListener(DeviceService deviceService, ObjectMapper objectMapper) {
        this.deviceService = deviceService;
        this.objectMapper = objectMapper;
    }

    // the consumer group is spring.kafka.consumer.group-id = monitoring-service (see application.properties)
    @KafkaListener(topics = KafkaTopics.SYNC_EVENTS)
    public void onSyncEvent(String message) {
        LOGGER.info("Received sync event: {}", message);
        SyncEvent event;
        try {
            event = objectMapper.readValue(message, SyncEvent.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Invalid sync event, skipped: {}", message);
            return;
        }
        switch (event.getType()) {
            case DEVICE_CREATED:
            case DEVICE_UPDATED:
                deviceService.saveDevice(event.getId(), event.getUserId());
                break;
            case DEVICE_DELETED:
                deviceService.deleteDevice(event.getId());
                break;
            case USER_DELETED:
                deviceService.unassignUser(event.getId());
                break;
            default:
                // USER_CREATED: nothing to do, the owner of a device comes with the DEVICE_* events
                break;
        }
    }
}
