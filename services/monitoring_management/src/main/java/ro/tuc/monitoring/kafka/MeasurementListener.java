package ro.tuc.monitoring.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ro.tuc.common.events.KafkaTopics;
import ro.tuc.monitoring.dtos.MeasurementDTO;
import ro.tuc.monitoring.services.ConsumptionService;

/** Reads the "device-measurements" topic: one message per device every 10 (simulated) minutes. */
@Component
public class MeasurementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeasurementListener.class);

    private final ConsumptionService consumptionService;
    private final ObjectMapper objectMapper;

    public MeasurementListener(ConsumptionService consumptionService, ObjectMapper objectMapper) {
        this.consumptionService = consumptionService;
        this.objectMapper = objectMapper;
    }

    // the consumer group is spring.kafka.consumer.group-id = monitoring-service (see application.properties)
    @KafkaListener(topics = KafkaTopics.DEVICE_MEASUREMENTS)
    public void onMeasurement(String message) {
        // the assignment asks to display every message taken from the broker
        LOGGER.info("Received measurement: {}", message);
        MeasurementDTO measurement;
        try {
            measurement = objectMapper.readValue(message, MeasurementDTO.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Invalid measurement, skipped: {}", message);
            return;
        }
        // the message comes from outside the system, so check it before using it
        if (measurement.getDeviceId() == null || measurement.getTimestamp() == null) {
            LOGGER.error("Measurement without device_id or timestamp, skipped: {}", message);
            return;
        }
        consumptionService.addMeasurement(measurement);
    }
}
