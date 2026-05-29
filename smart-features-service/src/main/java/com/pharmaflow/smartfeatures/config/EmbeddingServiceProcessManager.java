package com.pharmaflow.smartfeatures.config;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingServiceProcessManager implements SmartLifecycle {

  private static final Logger logger =
      LoggerFactory.getLogger(EmbeddingServiceProcessManager.class);
  private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

  private final boolean embeddingEnabled;
  private final boolean autoStartEnabled;
  private final String serviceUrl;
  private final Path serviceDirectory;
  private final String pythonCommand;
  private final String modelName;
  private final int startupTimeoutSeconds;
  private final boolean failOnStartupFailure;
  private final HttpClient httpClient;

  private volatile boolean running;
  private Process process;

  public EmbeddingServiceProcessManager(
      @Value("${smartfeatures.embedding.enabled:true}") boolean embeddingEnabled,
      @Value("${smartfeatures.embedding-service.auto-start:true}") boolean autoStartEnabled,
      @Value("${smartfeatures.embedding-service.url:http://localhost:8000}") String serviceUrl,
      @Value("${smartfeatures.embedding-service.directory:../embedding-service}")
          String serviceDirectory,
      @Value("${smartfeatures.embedding-service.python-command:python3}") String pythonCommand,
      @Value("${smartfeatures.embedding-service.model-name:sentence-transformers/all-MiniLM-L6-v2}")
          String modelName,
      @Value("${smartfeatures.embedding-service.startup-timeout-seconds:90}")
          int startupTimeoutSeconds,
      @Value("${smartfeatures.embedding-service.fail-on-startup-failure:true}")
          boolean failOnStartupFailure) {
    this.embeddingEnabled = embeddingEnabled;
    this.autoStartEnabled = autoStartEnabled;
    this.serviceUrl = serviceUrl;
    this.serviceDirectory = resolveServiceDirectory(serviceDirectory);
    this.pythonCommand = pythonCommand;
    this.modelName = modelName;
    this.startupTimeoutSeconds = startupTimeoutSeconds;
    this.failOnStartupFailure = failOnStartupFailure;
    this.httpClient = HttpClient.newBuilder().connectTimeout(HEALTH_TIMEOUT).build();
  }

  @Override
  public void start() {
    if (!embeddingEnabled || !autoStartEnabled) {
      logger.info("Embedding service auto-start is disabled.");
      running = false;
      return;
    }

    if (isHealthy()) {
      logger.info("Embedding service is already running at {}.", serviceUrl);
      running = true;
      return;
    }

    try {
      ensureVirtualEnvironment();
      startEmbeddingProcess();
      waitUntilHealthy();
      running = true;
      logger.info("Embedding service started at {}.", serviceUrl);
    } catch (Exception ex) {
      running = false;
      stopManagedProcess();
      logger.error("Failed to start embedding service from {}.", serviceDirectory, ex);
      if (failOnStartupFailure) {
        throw new IllegalStateException("Failed to start required embedding service.", ex);
      }
    }
  }

  @Override
  public void stop() {
    running = false;
    stopManagedProcess();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public int getPhase() {
    return Integer.MIN_VALUE;
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @PreDestroy
  public void destroy() {
    stop();
  }

  private void ensureVirtualEnvironment()
      throws IOException, InterruptedException, NoSuchAlgorithmException {
    if (!Files.isDirectory(serviceDirectory)) {
      throw new IOException("Embedding service directory does not exist: " + serviceDirectory);
    }

    Path venvDirectory = serviceDirectory.resolve(".venv");
    Path venvPython = venvPython(venvDirectory);
    if (!Files.exists(venvPython)) {
      logger.info("Creating embedding service virtual environment at {}.", venvDirectory);
      runCommand(List.of(pythonCommand, "-m", "venv", venvDirectory.toString()));
    }

    Path requirements = serviceDirectory.resolve("requirements.txt");
    if (!Files.exists(requirements)) {
      throw new IOException("Embedding service requirements file is missing: " + requirements);
    }

    String requirementsHash = sha256(requirements);
    Path marker = venvDirectory.resolve(".requirements.sha256");
    String installedHash =
        Files.exists(marker) ? Files.readString(marker, StandardCharsets.UTF_8).trim() : "";

    if (!requirementsHash.equals(installedHash)) {
      logger.info("Installing embedding service Python dependencies.");
      runCommand(
          List.of(
              venvPython.toString(),
              "-m",
              "pip",
              "install",
              "-r",
              requirements.toString()));
      Files.writeString(marker, requirementsHash, StandardCharsets.UTF_8);
    }

    ensureModelCached(venvPython, venvDirectory);
  }

  private void startEmbeddingProcess() throws IOException {
    URI uri = URI.create(serviceUrl);
    String host = Optional.ofNullable(uri.getHost()).orElse("localhost");
    int port = uri.getPort() > 0 ? uri.getPort() : 8000;

    ProcessBuilder processBuilder =
        new ProcessBuilder(
            venvPython(serviceDirectory.resolve(".venv")).toString(),
            "-m",
            "uvicorn",
            "app.main:app",
            "--host",
            host,
            "--port",
            String.valueOf(port));
    processBuilder.directory(serviceDirectory.toFile());
    processBuilder.environment().put("EMBEDDING_MODEL_LOCAL_FILES_ONLY", "true");
    processBuilder.redirectErrorStream(true);
    process = processBuilder.start();
    streamProcessOutput(process);
  }

  private void ensureModelCached(Path venvPython, Path venvDirectory)
      throws IOException, InterruptedException {
    Path marker = venvDirectory.resolve(".embedding-model.cached");
    if (Files.exists(marker) && canLoadModelFromCache(venvPython)) {
      return;
    }

    if (!canLoadModelFromCache(venvPython)) {
      logger.info("Caching embedding model {}. First startup may take a while.", modelName);
      runCommand(List.of(venvPython.toString(), "-c", modelLoadScript(false)));
    }
    Files.writeString(marker, modelName, StandardCharsets.UTF_8);
  }

  private boolean canLoadModelFromCache(Path venvPython) {
    try {
      runCommand(List.of(venvPython.toString(), "-c", modelLoadScript(true)));
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  private void waitUntilHealthy() throws InterruptedException {
    long deadline = System.nanoTime() + Duration.ofSeconds(startupTimeoutSeconds).toNanos();
    while (System.nanoTime() < deadline) {
      if (process != null && !process.isAlive()) {
        throw new IllegalStateException("Embedding service process exited during startup.");
      }
      if (isHealthy()) {
        return;
      }
      Thread.sleep(1000);
    }
    throw new IllegalStateException(
        "Embedding service did not become healthy within "
            + startupTimeoutSeconds
            + " seconds.");
  }

  private boolean isHealthy() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(serviceUrl + "/health"))
              .timeout(HEALTH_TIMEOUT)
              .GET()
              .build();
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() >= 200 && response.statusCode() < 300;
    } catch (Exception ex) {
      return false;
    }
  }

  private void runCommand(List<String> command) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(serviceDirectory.toFile());
    processBuilder.redirectErrorStream(true);
    Process commandProcess = processBuilder.start();

    String output;
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(commandProcess.getInputStream(), StandardCharsets.UTF_8))) {
      output = String.join(System.lineSeparator(), reader.lines().toList());
    }

    int exitCode = commandProcess.waitFor();
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Command failed with exit code "
              + exitCode
              + ": "
              + String.join(" ", command)
              + System.lineSeparator()
              + output);
    }
  }

  private void streamProcessOutput(Process startedProcess) {
    Thread logThread =
        new Thread(
            () -> {
              try (BufferedReader reader =
                  new BufferedReader(
                      new InputStreamReader(
                          startedProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                  logger.info("[embedding-service] {}", line);
                }
              } catch (IOException ex) {
                logger.debug("Embedding service log stream closed.", ex);
              }
            },
            "embedding-service-output");
    logThread.setDaemon(true);
    logThread.start();
  }

  private void stopManagedProcess() {
    if (process == null || !process.isAlive()) {
      return;
    }
    logger.info("Stopping managed embedding service process.");
    process.destroy();
    try {
      if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }

  private Path venvPython(Path venvDirectory) {
    if (System.getProperty("os.name").toLowerCase().contains("win")) {
      return venvDirectory.resolve("Scripts").resolve("python.exe");
    }
    return venvDirectory.resolve("bin").resolve("python");
  }

  private String sha256(Path file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
  }

  private Path resolveServiceDirectory(String configuredDirectory) {
    Path configured = Path.of(configuredDirectory);
    if (configured.isAbsolute() && Files.isDirectory(configured)) {
      return configured.normalize();
    }

    Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    List<Path> candidates =
        List.of(
            userDir.resolve(configuredDirectory),
            userDir.resolve("../embedding-service"),
            userDir.resolve("embedding-service"),
            userDir.resolve("pharmaflow-backend/embedding-service"));

    return candidates.stream()
        .map(Path::normalize)
        .filter(Files::isDirectory)
        .findFirst()
        .orElseGet(() -> userDir.resolve(configuredDirectory).normalize());
  }

  private String modelLoadScript(boolean localFilesOnly) {
    return "from sentence_transformers import SentenceTransformer; "
        + "SentenceTransformer("
        + pythonStringLiteral(modelName)
        + ", local_files_only="
        + (localFilesOnly ? "True" : "False")
        + ")";
  }

  private String pythonStringLiteral(String value) {
    return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
  }
}
