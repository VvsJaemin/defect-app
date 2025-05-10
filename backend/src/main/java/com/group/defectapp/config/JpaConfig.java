package com.group.defectapp.config;

import com.group.defectapp.util.AES256Cipher;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Objects;


@Configuration
@EnableJpaRepositories(
        basePackages = "com.group.defectapp.repository", // JPA repository 경로
        entityManagerFactoryRef = "defectEntityManagerFactory",
        transactionManagerRef = "defectTransactionManager"
)
public class JpaConfig {

    @Autowired
    private Environment env;

    @Primary
    @Bean(name = "defectDataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource defectDataSource() throws Exception {
        return org.springframework.boot.jdbc.DataSourceBuilder.create()
                .driverClassName(Objects.requireNonNull(env.getProperty("spring.datasource.driver-class-name")))
                .url(Objects.requireNonNull(env.getProperty("spring.datasource.url")))
                .username(Objects.requireNonNull(env.getProperty("spring.datasource.username")))
                .password(AES256Cipher.getINSTANCE().decode(
                        Objects.requireNonNull(env.getProperty("spring.datasource.password"))
                ))
                .build();
    }

    @Primary
    @Bean(name = "defectEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean defectEntityManagerFactory(
            EntityManagerFactoryBuilder builder) throws Exception {
        return builder
                .dataSource(defectDataSource())
                .packages("com.group.defectapp.domain") // JPA entity 경로
                .persistenceUnit("defectPU")
                .build();
    }

    @Primary
    @Bean(name = "defectTransactionManager")
    public PlatformTransactionManager defectTransactionManager(
            EntityManagerFactory defectEntityManagerFactory) {
        return new JpaTransactionManager(defectEntityManagerFactory);
    }
}
