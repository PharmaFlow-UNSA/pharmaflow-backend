package com.pharmaflow.orderprescription.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupSummaryLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupSummaryLogger.class);

    private final Environment environment;
    private final ObjectProvider<DataSource> dataSourceProvider;

    public StartupSummaryLogger(Environment environment, ObjectProvider<DataSource> dataSourceProvider) {
        this.environment = environment;
        this.dataSourceProvider = dataSourceProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupSummary() {
        log.info("Startup: service={}, version={}, profiles={}, port={}", appName(), version(), profiles(), port());
        logDatabaseStatus();
        log.info("Modules: orders, order items, prescriptions, payments, auto-refills, order fulfillment saga");
        log.info("Demo order data: seeded on startup when repositories are empty and profile is not test");
        log.info(
                "Discovery: eureka={}, register={}, fetchRegistry={}",
                enabled("eureka.client.enabled", true),
                enabled("eureka.client.register-with-eureka", true),
                enabled("eureka.client.fetch-registry", true));
        log.info(
                "RabbitMQ saga: enabled={}, exchangeConfigured={}, fulfillmentQueuesConfigured={}",
                enabled("pharmaflow.rabbitmq.enabled", true),
                hasText(environment.getProperty("pharmaflow.rabbitmq.exchange")),
                hasText(environment.getProperty("pharmaflow.rabbitmq.order-stock-reserved-queue")));
    }

    private void logDatabaseStatus() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        if (dataSource == null) {
            log.warn("Database: not configured");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            log.info("Database: connected, product={}, version={}", metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion());
        } catch (Exception ex) {
            log.error("Database: connection check failed - {}", ex.getMessage());
        }
    }

    private String appName() {
        return environment.getProperty("spring.application.name", "order-prescription-service");
    }

    private String port() {
        return environment.getProperty("server.port", "8087");
    }

    private String profiles() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
    }

    private String version() {
        return Arrays.stream(new String[] {
                        environment.getProperty("info.app.version"),
                        StartupSummaryLogger.class.getPackage().getImplementationVersion()
                })
                .filter(this::hasText)
                .findFirst()
                .orElse("dev");
    }

    private boolean enabled(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
