package ro.tuc.ds2020.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;
import ro.tuc.ds2020.services.PersonService;

/**
 * Reads the "sync-events" topic. user-service only reacts to USER_CREATED (published by auth-service on register):
 * it creates the person with the same id.
 */
@Component
public class SyncEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventListener.class);

    private final PersonService personService;
    private final ObjectMapper objectMapper;

    public SyncEventListener(PersonService personService, ObjectMapper objectMapper) {
        this.personService = personService;
        this.objectMapper = objectMapper;
    }

    // the consumer group is spring.kafka.consumer.group-id = user-service (see application.properties)
    @KafkaListener(topics = KafkaTopics.SYNC_EVENTS)
    public void onSyncEvent(String message) {
        SyncEvent event;
        try {
            event = objectMapper.readValue(message, SyncEvent.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Invalid sync event, skipped: {}", message);
            return;
        }
        // USER_DELETED is published by this service itself and DEVICE_* are not about persons: nothing to do
        if (event.getType() != SyncEventType.USER_CREATED) {
            return;
        }
        LOGGER.info("Received sync event: {}", message);
        // events from before this change carried only the id (user-service published them itself): no profile
        if (event.getUsername() == null) {
            LOGGER.warn("USER_CREATED without a profile, skipped: {}", message);
            return;
        }
        personService.insert(event);
    }
}
