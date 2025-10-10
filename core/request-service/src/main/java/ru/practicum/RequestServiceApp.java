package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.configuration.GlobalErrorHandlerConfig;
import ru.practicum.configuration.JacksonConfig;


@EnableFeignClients
@SpringBootApplication
@Import({GlobalErrorHandlerConfig.class, JacksonConfig.class})
public class RequestServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApp.class, args);
    }
}
