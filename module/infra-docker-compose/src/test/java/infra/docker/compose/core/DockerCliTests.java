/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.docker.compose.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import infra.app.logging.LogLevel;
import infra.docker.compose.core.DockerCli.DockerComposeOptions;
import infra.test.stdio.CapturedOutput;
import infra.test.stdio.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(OutputCaptureExtension.class)
class DockerCliTests {

  private static final String[] DOCKER_VERSION = { "docker", "version", "--format", "{{.Client.Version}}" };

  private static final String[] COMPOSE_VERSION = { "docker", "compose", "version", "--format", "json" };

  private static final String[] STANDALONE_VERSION = { "docker-compose", "version", "--format", "json" };

  @TempDir
  Path directory;

  @Test
  void appliesOptionsOnlyToComposeCommandsAndForwardsOutput(CapturedOutput output) throws IOException {
    File base = Files.createFile(this.directory.resolve("compose.yaml")).toFile().getCanonicalFile();
    File override = Files.createFile(this.directory.resolve("override.yaml")).toFile().getCanonicalFile();
    DockerComposeFile files = DockerComposeFile.of(List.of(base, override));
    DockerComposeOptions options = new DockerComposeOptions(files, Set.of("dev"), List.of("--project-name", "sample"));
    String[] contextCommand = { "docker", "context", "ls", "--format={{ json . }}" };
    String[] startCommand = { "docker", "compose", "--file", base.getPath(), "--file", override.getPath(),
            "--ansi", "never", "--profile", "dev", "--project-name", "sample", "start", "database" };
    try (var runners = mockConstruction(ProcessRunner.class, (runner, context) -> {
      assertThat(context.arguments()).isEqualTo(List.of(this.directory.toFile()));
      given(runner.run(DOCKER_VERSION)).willReturn("28.0.0");
      given(runner.run(COMPOSE_VERSION)).willReturn("{\"version\":\"v2.30.0\"}");
      given(runner.run(null, contextCommand)).willReturn("{\"Name\":\"default\",\"Current\":true,\"DockerEndpoint\":\"unix:///var/run/docker.sock\"}");
      given(runner.run(any(), eq(startCommand))).willAnswer(invocation -> {
        Consumer<String> consumer = invocation.getArgument(0);
        consumer.accept("Database is starting");
        return "";
      });
    })) {
      DockerCli cli = new DockerCli(this.directory.toFile(), options);

      assertThat(cli.getDockerComposeFile()).isSameAs(files);
      assertThat(cli.run(new DockerCliCommand.Context())).extracting(DockerCliContextResponse::name).containsExactly("default");
      assertThat(cli.run(new DockerCliCommand.ComposeStart(LogLevel.INFO, List.of("database"))))
              .isSameAs(DockerCliCommand.None.INSTANCE);
      assertThat(output).contains("Database is starting");
      then(runners.constructed().get(0)).should().run(any(), eq(startCommand));
    }
  }

  @Test
  void fallsBackToStandaloneComposeAndUsesItsVersionForPs() {
    String[] psCommand = { "docker-compose", "--ansi", "never", "ps", "--format=json" };
    try (var runners = mockConstruction(ProcessRunner.class, (runner, context) -> {
      given(runner.run(DOCKER_VERSION)).willReturn("28.0.0");
      given(runner.run(COMPOSE_VERSION)).willThrow(exitFailure(COMPOSE_VERSION, "compose plugin unavailable"));
      given(runner.run(STANDALONE_VERSION)).willReturn("{\"version\":\"v2.23.0\"}");
      given(runner.run(null, psCommand)).willReturn("[]");
    })) {
      DockerCli cli = new DockerCli(this.directory.toFile(), null);

      assertThat(cli.getDockerComposeFile()).isNull();
      assertThat(cli.run(new DockerCliCommand.ComposePs())).isEmpty();
      then(runners.constructed().get(0)).should().run(null, psCommand);
    }
  }

  @Test
  void reportsThatDockerCouldNotStart() {
    ProcessStartException failure = startFailure(DOCKER_VERSION);
    try (var runners = mockConstruction(ProcessRunner.class,
            (runner, context) -> given(runner.run(DOCKER_VERSION)).willThrow(failure))) {
      assertThatExceptionOfType(DockerProcessStartException.class)
              .isThrownBy(() -> new DockerCli(this.directory.toFile(), null))
              .withMessageContaining("Unable to start docker process").withCause(failure);
      then(runners.constructed().get(0)).should().run(DOCKER_VERSION);
      then(runners.constructed().get(0)).shouldHaveNoMoreInteractions();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "docker daemon is not running", "Cannot connect to the Docker daemon" })
  void reportsDaemonErrorOutput(String errorOutput) {
    ProcessExitException failure = exitFailure(DOCKER_VERSION, errorOutput);
    try (var runners = mockConstruction(ProcessRunner.class,
            (runner, context) -> given(runner.run(DOCKER_VERSION)).willThrow(failure))) {
      assertThatExceptionOfType(DockerNotRunningException.class)
              .isThrownBy(() -> new DockerCli(this.directory.toFile(), null))
              .withCause(failure)
              .satisfies(exception -> assertThat(exception.getErrorOutput()).isEqualTo(errorOutput));
    }
  }

  @Test
  void preservesOtherDockerFailures() {
    ProcessExitException failure = exitFailure(DOCKER_VERSION, "unexpected client error");
    try (var runners = mockConstruction(ProcessRunner.class,
            (runner, context) -> given(runner.run(DOCKER_VERSION)).willThrow(failure))) {
      assertThatExceptionOfType(ProcessExitException.class)
              .isThrownBy(() -> new DockerCli(this.directory.toFile(), null))
              .isSameAs(failure);
    }
  }

  @Test
  void reportsWhenNeitherComposeCommandCanRun() {
    ProcessStartException failure = startFailure(STANDALONE_VERSION);
    try (var runners = mockConstruction(ProcessRunner.class, (runner, context) -> {
      given(runner.run(DOCKER_VERSION)).willReturn("28.0.0");
      given(runner.run(COMPOSE_VERSION)).willThrow(exitFailure(COMPOSE_VERSION, "compose plugin unavailable"));
      given(runner.run(STANDALONE_VERSION)).willThrow(failure);
    })) {
      assertThatExceptionOfType(DockerProcessStartException.class)
              .isThrownBy(() -> new DockerCli(this.directory.toFile(), null))
              .withMessageContaining("Unable to start 'docker-compose' process or use 'docker compose'").withCause(failure);
    }
  }

  private static ProcessExitException exitFailure(String[] command, String errorOutput) {
    return new ProcessExitException(1, command, "", errorOutput);
  }

  private static ProcessStartException startFailure(String[] command) {
    return new ProcessStartException(command, new IOException("Executable not found"));
  }
}
