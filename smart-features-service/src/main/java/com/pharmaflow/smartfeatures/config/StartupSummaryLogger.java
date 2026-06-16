package com.pharmaflow.smartfeatures.config;

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
import org.springframework.util.ClassUtils;

@Component
public class StartupSummaryLogger {

  private static final Logger log = LoggerFactory.getLogger(StartupSummaryLogger.class);

  private final Environment environment;
  private final ObjectProvider<DataSource> dataSourceProvider;

  public StartupSummaryLogger(
      Environment environment, ObjectProvider<DataSource> dataSourceProvider) {
    this.environment = environment;
    this.dataSourceProvider = dataSourceProvider;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logStartupSummary() {
    log.info(
        "Startup: service={}, version={}, profiles={}, port={}",
        appName(),
        version(),
        profiles(),
        port());
    logDatabaseStatus();
    log.info(
        "Migrations: flywayPresent={}, baselineOnMigrate={}",
        ClassUtils.isPresent("org.flywaydb.core.Flyway", getClass().getClassLoader()),
        enabled("spring.flyway.baseline-on-migrate", false));
    log.info(
        "Modules: symptoms, recommendations, fraud checks, FAQ/chatbot, notifications, therapy reminders");
    log.info(
        "Notifications: enabled, provider=database, websocket=false, polling=frontend, channels=in-app/email/sms metadata");
    log.info(
        "Therapy reminders: enabled, scheduler=true, dispatchDelayMs={}",
        environment.getProperty("smartfeatures.reminders.dispatch-delay-ms", "60000"));
    log.info(
        "Embedding service: enabled={}, autoStart={}, modelConfigured={}, failOnStartupFailure={}",
        enabled("smartfeatures.embedding.enabled", true),
        enabled("smartfeatures.embedding-service.auto-start", true),
        hasText(environment.getProperty("smartfeatures.embedding-service.model-name")),
        enabled("smartfeatures.embedding-service.fail-on-startup-failure", true));
    log.info(
        "External service clients: product={}, order={}, user={}",
        environment.getProperty("smartfeatures.product-service.service-id", "product-health-service"),
        environment.getProperty("smartfeatures.order-service.service-id", "order-prescription-service"),
        environment.getProperty("smartfeatures.user-service.service-id", "user-health-service"));
    log.info(
        "RabbitMQ saga: enabled={}, exchangeConfigured={}, reservationQueuesConfigured={}",
        enabled("pharmaflow.rabbitmq.enabled", true),
        hasText(environment.getProperty("pharmaflow.rabbitmq.exchange")),
        hasText(environment.getProperty("pharmaflow.rabbitmq.smart-reservation-created-queue")));
    log.info(
        "Discovery: eureka={}, register={}, fetchRegistry={}",
        enabled("eureka.client.enabled", true),
        enabled("eureka.client.register-with-eureka", true),
        enabled("eureka.client.fetch-registry", true));
    log.info("Demo smart data: seeded on startup when profile is not test and symptom data is empty");
  }

  private void logDatabaseStatus() {
    DataSource dataSource = dataSourceProvider.getIfAvailable();
    if (dataSource == null) {
      log.warn("Database: not configured");
      return;
    }
    try (Connection connection = dataSource.getConnection()) {
      DatabaseMetaData metaData = connection.getMetaData();
      log.info(
          "Database: connected, product={}, version={}",
          metaData.getDatabaseProductName(),
          metaData.getDatabaseProductVersion());
    } catch (Exception ex) {
      log.error("Database: connection check failed - {}", ex.getMessage());
    }
  }

  private String appName() {
    return environment.getProperty("spring.application.name", "smart-features-service");
  }

  private String port() {
    return environment.getProperty("server.port", "8082");
  }

  private String profiles() {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
  }

  private String version() {
    return Arrays.stream(
            new String[] {
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
