package com.pharmaflow.config;

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
    log.info("Config source: native={}, searchLocations={}", nativeProfileEnabled(), safeLocations());
    log.info("Served service configs: user-health, product-health, order-prescription, pharmacy-inventory");
    log.info("Security: config server authentication not configured for local development");
  }

  private String appName() {
    return environment.getProperty("spring.application.name", "config-server");
  }

  private String port() {
    return environment.getProperty("server.port", "8888");
  }

  private String profiles() {
    String[] activeProfiles = environment.getActiveProfiles();
    return activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
  }

  private boolean nativeProfileEnabled() {
    return Arrays.asList(environment.getActiveProfiles()).contains("native")
        || environment.getProperty("spring.profiles.active", "").contains("native");
  }

  private String safeLocations() {
    return environment.getProperty("spring.cloud.config.server.native.search-locations", "classpath:/config");
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

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
