package ro.tuc.devices.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.common.events.SyncEvent;
import ro.tuc.common.handlers.exceptions.model.CustomException;

import java.util.List;

/** Sends synchronization events to the "sync-events" Kafka topic, as JSON text. */
@Component
public class SyncEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public SyncEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Sends the event and waits (get()) until Kafka confirms that it stored it.
     * If Kafka cannot be reached, the producer gives up after 10 s (delivery.timeout.ms in application.properties)
     * and this throws a 503 error, so the caller's @Transactional database change is rolled back.
     */
    public void publish(SyncEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            // the key (the device id) chooses the partition, so all events of one device stay in order
            kafkaTemplate.send(KafkaTopics.SYNC_EVENTS, event.getId().toString(), json).get();
            LOGGER.info("Published sync event: {}", json);
        } catch (InterruptedException e) {
            // keep the "interrupted" flag of the thread, then fail like for any other error
            Thread.currentThread().interrupt();
            throw kafkaUnavailable(event, e);
        } catch (Exception e) {
            throw kafkaUnavailable(event, e);
        }
    }

    private CustomException kafkaUnavailable(SyncEvent event, Exception e) {
        LOGGER.error("Could not publish sync event {} for {}", event.getType(), event.getId(), e);
        return new CustomException("Kafka is not reachable, the change was not saved",
                HttpStatus.SERVICE_UNAVAILABLE, "SyncEvent", List.of(String.valueOf(e.getMessage())));
    }
}
