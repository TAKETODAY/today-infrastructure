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

package infra.docker.compose.core;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.app.logging.LogLevel;
import infra.docker.compose.core.DockerCli.DockerComposeOptions;

/**
 * Provides a high-level API to work with Docker compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 5.0
 */
public interface DockerCompose {

  /**
   * Timeout duration used to request a forced stop.
   */
  Duration FORCE_STOP = Duration.ZERO;

  /**
   * Run {@code docker compose up} to create and start services. Waits until all
   * contains are started and healthy.
   *
   * @param logLevel the log level used to report progress
   */
  void up(LogLevel logLevel);

  /**
   * Run {@code docker compose up} to create and start services. Waits until all
   * contains are started and healthy.
   *
   * @param logLevel the log level used to report progress
   * @param arguments the arguments to pass to the up command
   */
  void up(LogLevel logLevel, List<String> arguments);

  /**
   * Run {@code docker compose down} to stop and remove any running services.
   *
   * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
   * waiting.
   */
  void down(Duration timeout);

  /**
   * Run {@code docker compose down} to stop and remove any running services.
   *
   * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
   * waiting.
   * @param arguments the arguments to pass to the down command
   */
  void down(Duration timeout, List<String> arguments);

  /**
   * Run {@code docker compose start} to start services. Waits until all containers are
   * started and healthy.
   *
   * @param logLevel the log level used to report progress
   */
  void start(LogLevel logLevel);

  /**
   * Run {@code docker compose start} to start services. Waits until all containers are
   * started and healthy.
   *
   * @param logLevel the log level used to report progress
   * @param arguments the arguments to pass to the start command
   */
  void start(LogLevel logLevel, List<String> arguments);

  /**
   * Run {@code docker compose stop} to stop any running services.
   *
   * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
   * waiting.
   */
  void stop(Duration timeout);

  /**
   * Run {@code docker compose stop} to stop any running services.
   *
   * @param timeout the amount of time to wait or {@link #FORCE_STOP} to stop without
   * waiting.
   * @param arguments the arguments to pass to the stop command
   */
  void stop(Duration timeout, List<String> arguments);

  /**
   * Run {@code docker compose logs} to view Docker Compose's recent logs.
   *
   * @return the recent logs
   */
  String logs();

  /**
   * Return if services have been defined in the {@link DockerComposeFile} for the
   * active profiles.
   *
   * @return {@code true} if services have been defined
   * @see #hasDefinedServices()
   */
  boolean hasDefinedServices();

  /**
   * Return the running services for the active profile, or an empty list if no services
   * are running.
   *
   * @return the list of running services
   */
  List<RunningService> getRunningServices();

  /**
   * Factory method used to create a {@link DockerCompose} instance.
   *
   * @param file the Docker Compose file
   * @param hostname the hostname used for services or {@code null} if the hostname
   * should be deduced
   * @param activeProfiles a set of the profiles that should be activated
   * @return a {@link DockerCompose} instance
   */
  static DockerCompose get(DockerComposeFile file, @Nullable String hostname, Set<String> activeProfiles) {
    return get(file, hostname, activeProfiles, Collections.emptyList());
  }

  /**
   * Factory method used to create a {@link DockerCompose} instance.
   *
   * @param file the Docker Compose file
   * @param hostname the hostname used for services or {@code null} if the hostname
   * should be deduced
   * @param activeProfiles a set of the profiles that should be activated
   * @param arguments the arguments to pass to Docker Compose
   * @return a {@link DockerCompose} instance
   */
  static DockerCompose get(DockerComposeFile file, @Nullable String hostname, Set<String> activeProfiles,
          List<String> arguments) {
    DockerCli cli = new DockerCli(null, new DockerComposeOptions(file, activeProfiles, arguments));
    return new DefaultDockerCompose(cli, hostname);
  }

}
