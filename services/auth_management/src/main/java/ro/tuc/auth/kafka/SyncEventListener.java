package ro.tuc.auth.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.tuc.auth.services.AuthService;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.events.SyncEventType;

/**
 * Reads the "sync-events" topic. auth-service only reacts to USER_DELETED (published by user-service):
 * it deletes the login of that user.
 */
@Component
public class SyncEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventListener.class);

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public SyncEventListener(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    // the consumer group is spring.kafka.consumer.group-id = auth-service
    //metoda apelata automat cand se publica un mesaj nou pe topic
    @KafkaListener(topics = KafkaTopics.SYNC_EVENTS)
    public void onSyncEvent(String message) {
        SyncEvent event;
        try {
            event = objectMapper.readValue(message, SyncEvent.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Invalid sync event, skipped: {}", message);
            return;
        }
        // USER_CREATED is published by this service itself and DEVICE_* are not about logins: nothing to do
        if (event.getType() == SyncEventType.USER_DELETED) {
            LOGGER.info("Received sync event: {}", message);
            authService.deleteByUserId(event.getId());
        }
    }
}
