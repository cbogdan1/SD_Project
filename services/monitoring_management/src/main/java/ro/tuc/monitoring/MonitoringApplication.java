package ro.tuc.monitoring;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ro.tuc.common.handlers.RestExceptionHandler;

import java.util.TimeZone;

@SpringBootApplication
@Import(RestExceptionHandler.class)
// Swagger "Authorize" button: paste the JWT from /auth/login, Swagger sends it as "Authorization: Bearer <token>"
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class MonitoringApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MonitoringApplication.class, args);
    }
}
