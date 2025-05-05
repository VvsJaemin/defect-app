package com.group.defectapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "결함 관리 시스템 API",
                    description = "결함 관리 시스템 API입니다.",
                    version = "v1.0"
        )
)
@RequiredArgsConstructor
@Configuration
public class SwaggerConfig {

}
