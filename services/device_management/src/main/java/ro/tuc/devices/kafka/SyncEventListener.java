package ro.tuc.devices.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.devices.services.DeviceService;

/**
 * Reads the "sync-events" topic. device-service keeps a copy of the user ids (app_user table),
 * so it only reacts to USER_CREATED and USER_DELETED.
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

    // the consumer group is spring.kafka.consumer.group-id = device-service (see application.properties)
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
            case USER_CREATED:
                deviceService.addUser(event.getId());
                break;
            case USER_DELETED:
                deviceService.deleteUser(event.getId());
                break;
            default:
                // DEVICE_* events are published by this service itself: nothing to do here
                break;
        }
    }
}
