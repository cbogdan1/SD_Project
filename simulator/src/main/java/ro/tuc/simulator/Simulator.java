package ro.tuc.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Device Data Simulator (the producer). It runs on the host machine, outside Docker:
 *     java -jar target/simulator.jar config.properties
 * Each step it creates one measurement (the energy used in 10 minutes) and sends it to Kafka as JSON.
 */
public final class Simulator {

    private static final int MINUTES_PER_MEASUREMENT = 10;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Simulator() {
    }

    public static void main(String[] args) throws Exception {
        // the config file can be given as the first argument; default: config.properties in the current folder
        String configFile = args.length > 0 ? args[0] : "config.properties";
        Properties config = new Properties();
        try (FileInputStream in = new FileInputStream(configFile)) {
            config.load(in);
        }
        // stops with "Invalid UUID string" if device.id is missing or wrong
        String deviceId = UUID.fromString(config.getProperty("device.id", "").trim()).toString();
        String kafkaServers = config.getProperty("kafka.bootstrap.servers", "localhost:9092").trim();
        String topic = config.getProperty("kafka.topic", "device-measurements").trim();
        long intervalSeconds = Long.parseLong(config.getProperty("send.interval.seconds", "600").trim());
        LocalDateTime time = startTime(config.getProperty("start.time", ""));

        EnergyGenerator generator = new EnergyGenerator();
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("Simulating device " + deviceId + ": one 10-minute measurement every "
                + intervalSeconds + " s to " + kafkaServers + ", topic " + topic + " (Ctrl+C to stop)");

        try (KafkaProducer<String, String> producer = createProducer(kafkaServers)) {
            while (true) {
                // LinkedHashMap keeps the fields in this order in the JSON
                Map<String, Object> measurement = new LinkedHashMap<>();
                measurement.put("timestamp", time.format(TIME_FORMAT));
                measurement.put("device_id", deviceId);
                measurement.put("measurement_value", generator.nextValue(time));
                String json = objectMapper.writeValueAsString(measurement);

                // key = device id, so all the measurements of one device go to the same partition (in order).
                // get() waits until Kafka confirms the message, so "Sent" is printed only for stored messages.
                producer.send(new ProducerRecord<>(topic, deviceId, json)).get();
                System.out.println("Sent: " + json);

                // the simulated clock always moves 10 minutes, even when we wait only 1 s (demo mode)
                time = time.plusMinutes(MINUTES_PER_MEASUREMENT);
                Thread.sleep(intervalSeconds * 1000);
            }
        }
    }

    private static KafkaProducer<String, String> createProducer(String kafkaServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServers);
        // the key (device id) and the value (the JSON) are sent as text
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    // start.time from the config (e.g. 2026-11-22T00:00); empty = now, rounded down to 10 minutes (18:47 -> 18:40)
    private static LocalDateTime startTime(String value) {
        if (value.isBlank()) {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            return now.minusMinutes(now.getMinute() % MINUTES_PER_MEASUREMENT);
        }
        return LocalDateTime.parse(value.trim());
    }
}
