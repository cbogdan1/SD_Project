package ro.tuc.devices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ro.tuc.common.handlers.RestExceptionHandler;

import java.util.TimeZone;

@SpringBootApplication
@Import(RestExceptionHandler.class)
public class DevicesApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(DevicesApplication.class, args);
    }
}
