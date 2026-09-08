package com.mini.credit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniCreditOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mini Credit API")
                        .description("API de gestion de micro-credit")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("aimeB")));
    }
}