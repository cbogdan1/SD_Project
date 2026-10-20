package ro.tuc.ds2020;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import ro.tuc.common.handlers.RestExceptionHandler;

import java.util.TimeZone;

@SpringBootApplication
@Import(RestExceptionHandler.class)
@Validated
public class UsersApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(UsersApplication.class);
    }

    public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(UsersApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
