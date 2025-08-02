package com.group.defectapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI qualityManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("품질관리시스템(QMS) API")
                        .description("품질관리시스템 API 문서")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("박재민")
                                .url("https://qms.jaemin.app")
                                .email("djd453216@nate.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

}
