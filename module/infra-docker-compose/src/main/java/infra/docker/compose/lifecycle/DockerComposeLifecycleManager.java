/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.docker.compose.lifecycle;

import org.jspecify.annotations.Nullable;
import infra.docker.compose.core.DockerCompose;
import infra.docker.compose.core.DockerComposeFile;
import infra.docker.compose.core.RunningService;
import infra.docker.compose.lifecycle.DockerComposeProperties.Readiness.Wait;
import infra.docker.compose.lifecycle.DockerComposeProperties.Start;
import infra.docker.compose.lifecycle.DockerComposeProperties.Start.Skip;
import infra.docker.compose.lifecycle.DockerComposeProperties.Stop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import infra.aot.AotDetector;
import infra.app.ApplicationShutdownHandlers;
import infra.context.ApplicationContext;
import infra.context.ApplicationListener;
import infra.context.aot.AbstractAotProcessor;
import infra.context.event.SimpleApplicationEventMulticaster;
import infra.lang.Assert;
import infra.logging.LogMessage;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;

/**
 * Manages the lifecycle for Docker Compose services.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @see DockerComposeListener
 */
class DockerComposeLifecycleManager {

  private static final Logger logger = LoggerFactory.getLogger(DockerComposeLifecycleManager.class);

  private static final String IGNORE_LABEL = "infra.ignore";

  private final @Nullable File workingDirectory;

  private final ApplicationContext applicationContext;

  private final @Nullable ClassLoader classLoader;

  private final ApplicationShutdownHandlers shutdownHandlers;

  private final DockerComposeProperties properties;

  private final Set<ApplicationListener<?>> eventListeners;

  private final DockerComposeSkipCheck skipCheck;

  private final ServiceReadinessChecks serviceReadinessChecks;

  DockerComposeLifecycleManager(ApplicationContext applicationContext,
          ApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
          Set<ApplicationListener<?>> eventListeners) {
    this(null, applicationContext, shutdownHandlers, properties, eventListeners, new DockerComposeSkipCheck(),
            null);
  }

  DockerComposeLifecycleManager(@Nullable File workingDirectory, ApplicationContext applicationContext,
          ApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
          Set<ApplicationListener<?>> eventListeners, DockerComposeSkipCheck skipCheck,
          @Nullable ServiceReadinessChecks serviceReadinessChecks) {
    this.workingDirectory = workingDirectory;
    this.applicationContext = applicationContext;
    this.classLoader = applicationContext.getClassLoader();
    this.shutdownHandlers = shutdownHandlers;
    this.properties = properties;
    this.eventListeners = eventListeners;
    this.skipCheck = skipCheck;
    this.serviceReadinessChecks = (serviceReadinessChecks != null) ? serviceReadinessChecks
            : new ServiceReadinessChecks(properties.getReadiness());
  }

  void start() {
    if (Boolean.getBoolean(AbstractAotProcessor.AOT_PROCESSING) || AotDetector.useGeneratedArtifacts()) {
      logger.trace("Docker Compose support disabled with AOT and native images");
      return;
    }
    if (!this.properties.isEnabled()) {
      logger.trace("Docker Compose support not enabled");
      return;
    }
    if (this.skipCheck.shouldSkip(this.classLoader, this.properties.getSkip())) {
      logger.trace("Docker Compose support skipped");
      return;
    }
    DockerComposeFile composeFile = getComposeFile();
    Set<String> activeProfiles = this.properties.getProfiles().getActive();
    List<String> arguments = this.properties.getArguments();
    DockerCompose dockerCompose = getDockerCompose(composeFile, activeProfiles, arguments);
    if (!dockerCompose.hasDefinedServices()) {
      logger.warn(LogMessage.format("No services defined in Docker Compose file %s with active profiles %s",
              composeFile, activeProfiles));
      return;
    }
    LifecycleManagement lifecycleManagement = this.properties.getLifecycleManagement();
    Start start = this.properties.getStart();
    Stop stop = this.properties.getStop();
    Wait wait = this.properties.getReadiness().getWait();
    List<RunningService> runningServices = dockerCompose.getRunningServices();
    if (lifecycleManagement.shouldStart()) {
      Skip skip = this.properties.getStart().getSkip();
      if (skip.shouldSkip(runningServices)) {
        logger.info(skip.getLogMessage());
      }
      else {
        try {
          start.getCommand().applyTo(dockerCompose, start.getLogLevel(), start.getArguments());
        }
        catch (RuntimeException ex) {
          logDockerComposeLogs(dockerCompose, start);
          throw ex;
        }
        runningServices = dockerCompose.getRunningServices();
        if (wait == Wait.ONLY_IF_STARTED) {
          wait = Wait.ALWAYS;
        }
        if (lifecycleManagement.shouldStop()) {
          this.shutdownHandlers
                  .add(() -> stop.getCommand().applyTo(dockerCompose, stop.getTimeout(), stop.getArguments()));
        }
      }
    }
    List<RunningService> relevantServices = new ArrayList<>(runningServices);
    relevantServices.removeIf(this::isIgnored);
    if (wait == Wait.ALWAYS || wait == null) {
      this.serviceReadinessChecks.waitUntilReady(relevantServices);
    }
    publishEvent(new DockerComposeServicesReadyEvent(this.applicationContext, relevantServices));
  }

  private void logDockerComposeLogs(DockerCompose dockerCompose, Start start) {
    String command = start.getCommand().name().toLowerCase(Locale.ROOT);
    String logs = retrieveLogsIfPossible(dockerCompose);
    if (logs == null) {
      start.getLogLevel()
              .log(logger, "docker compose %s failed and its logs were unavailable".formatted(command));
    }
    else {
      start.getLogLevel()
              .log(logger, "docker compose %s failed with the following logs:%n%n%s".formatted(command, logs));
    }
  }

  private @Nullable String retrieveLogsIfPossible(DockerCompose dockerCompose) {
    try {
      return dockerCompose.logs();
    }
    catch (Exception ex) {
      return null;
    }
  }

  protected DockerComposeFile getComposeFile() {
    DockerComposeFile composeFile = (CollectionUtils.isEmpty(this.properties.getFile()))
            ? DockerComposeFile.find(this.workingDirectory) : DockerComposeFile.of(this.properties.getFile());
    Assert.state(composeFile != null, () -> "No Docker Compose file found in directory '%s'".formatted(
            ((this.workingDirectory != null) ? this.workingDirectory : new File(".")).toPath().toAbsolutePath()));
    if (composeFile.getFiles().size() == 1) {
      logger.info(LogMessage.format("Using Docker Compose file %s", composeFile.getFiles().get(0)));
    }
    else {
      logger.info(LogMessage.format("Using Docker Compose files %s", composeFile.toString()));
    }
    return composeFile;
  }

  protected DockerCompose getDockerCompose(DockerComposeFile composeFile, Set<String> activeProfiles,
          List<String> arguments) {
    return DockerCompose.get(composeFile, this.properties.getHost(), activeProfiles, arguments);
  }

  private boolean isIgnored(RunningService service) {
    return service.labels().containsKey(IGNORE_LABEL);
  }

  /**
   * Publish a {@link DockerComposeServicesReadyEvent} directly to the event listeners
   * since we cannot call {@link ApplicationContext#publishEvent} this early.
   *
   * @param event the event to publish
   */
  private void publishEvent(DockerComposeServicesReadyEvent event) {
    SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
    this.eventListeners.forEach(multicaster::addApplicationListener);
    multicaster.multicastEvent(event);
  }

}
