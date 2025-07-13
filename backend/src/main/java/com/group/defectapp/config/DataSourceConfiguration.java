package com.group.defectapp.config;

import com.group.defectapp.util.AES256Cipher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean
    @Profile("local")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource localDataSource(Environment env) throws Exception {
        String encryptedPassword = env.getProperty("spring.datasource.password");
        String decryptedPassword = AES256Cipher.getINSTANCE().decode(encryptedPassword);

        return DataSourceBuilder.create()
                .driverClassName(env.getProperty("spring.datasource.driver-class-name"))
                .url(env.getProperty("spring.datasource.url"))
                .username(env.getProperty("spring.datasource.username"))
                .password(decryptedPassword)
                .build();
    }
}