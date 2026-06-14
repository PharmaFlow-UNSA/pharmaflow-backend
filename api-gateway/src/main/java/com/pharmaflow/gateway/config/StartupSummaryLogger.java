package com.pharmaflow.gateway.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupSummaryLogger {

  private static final Logger log = LoggerFactory.getLogger(StartupSummaryLogger.class);

  private final Environment environment;

  public StartupSummaryLogger(Environment environment) {
    this.environment = environment;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logStartupSummary() {
    log.info("Startup: service={}, version={}, profiles={}, port={}", appName(), version(), profiles(), port());
    log.info("Gateway routes: user-health, product-health, order-prescription, pharmacy-inventory, smart-features");
    log.info("Security: JWT authentication enabled, protected route filters active");
    log.info(
        "Discovery: eureka={}, defaultZoneConfigured={}",
        enabled("eureka.client.enabled", true),
        hasText(environment.getProperty("eureka.client.service-url.defaultZone")));
    log.info(
        "Redis: enabled={}, healthIndicator={}",
        enabled("redis.enabled", false),
        enabled("management.health.redis.enabled", false));
    log.info("Resilience: circuit breakers enabled for gateway routes");
  }

  private String appName() {
    return environment.getProperty("spring.application.name", "api-gateway");
  }

  private String port() {
    return environment.getProperty("server.port", "8080");
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
