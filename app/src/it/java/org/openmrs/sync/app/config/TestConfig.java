package org.openmrs.sync.app.config;

import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.openmrs.sync.component.config.ReceiverEncryptionProperties;
import org.openmrs.sync.component.config.SenderEncryptionProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories(
        entityManagerFactoryRef = "entityManager",
        basePackages = {"org.openmrs.sync.component.repository"}
)
@EntityScan("org.openmrs.sync.component.entity")
@ComponentScan({
        "org.openmrs.sync.component.service",
        "org.openmrs.sync.component.mapper",
        "org.openmrs.sync.component.camel",
        "org.openmrs.utils.odoo"
})
public class TestConfig {

    @Value("${spring.datasource.dialect}")
    private String hibernateDialect;

    @Value("${spring.datasource.ddlAuto}")
    private String ddlAuto;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource openmrsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "pgp.receiver")
    public ReceiverEncryptionProperties receiverEncryptionProperties() {
        return new ReceiverEncryptionProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "pgp.sender")
    public SenderEncryptionProperties senderProperties() {
        return new SenderEncryptionProperties();
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManager(final EntityManagerFactoryBuilder builder,
                                                                final DataSource dataSource) {
        Map<String, String> props = new HashMap<>();
        props.put("hibernate.dialect", hibernateDialect);
        props.put("hibernate.hbm2ddl.auto", ddlAuto);

        return builder
                .dataSource(dataSource)
                .packages("org.openmrs.sync.component.entity")
                .persistenceUnit("openmrs")
                .properties(props)
                .build();
    }

    @Bean
    public DeadLetterChannelBuilder deadLetterChannelBuilder() {
        DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder("direct:dlc");
        builder.setUseOriginalMessage(true);
        return builder;
    }
}
