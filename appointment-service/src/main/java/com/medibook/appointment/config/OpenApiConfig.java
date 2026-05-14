package com.medibook.appointment.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediBook Appointment Service API")
                        .version("1.0")
                        .description("Appointment booking and lifecycle APIs"));
    }
}