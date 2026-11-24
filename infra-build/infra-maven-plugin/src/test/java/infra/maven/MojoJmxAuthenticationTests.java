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

package infra.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

import infra.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MojoJmxAuthenticationTests {

  @TempDir
  Path directory;

  @Test
  void startConfiguresLocalJmxWithCredentialsReadableByStop() throws Exception {
    Path buildDirectory = this.directory.resolve("build output");
    Path passwordFile = buildDirectory.resolve(InfraApplicationAdminClient.JMX_PASSWORD_FILE);
    Path accessFile = buildDirectory.resolve("infra.jmxremote.access");
    StartMojo start = new StartMojo();
    ReflectionTestUtils.setField(start, "buildDirectory", buildDirectory.toFile());
    ReflectionTestUtils.setField(start, "jmxPort", 9001);

    assertThat(start.resolveJvmArguments().asArray()).contains(
            "-Dcom.sun.management.jmxremote.port=9001",
            "-Dcom.sun.management.jmxremote.rmi.port=9001",
            "-Dcom.sun.management.jmxremote.local.only=true",
            "-Dcom.sun.management.jmxremote.host=127.0.0.1",
            "-Dcom.sun.management.jmxremote.authenticate=true",
            "-Dcom.sun.management.jmxremote.password.file=" + passwordFile,
            "-Dcom.sun.management.jmxremote.access.file=" + accessFile);
    assertThat(Files.readAllLines(accessFile)).containsExactly(InfraApplicationAdminClient.JMX_USER + " readwrite");
    if (Files.getFileStore(buildDirectory).supportsFileAttributeView("posix")) {
      assertThat(Files.getPosixFilePermissions(passwordFile)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
      assertThat(Files.getPosixFilePermissions(accessFile)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
    }
    Properties credentials = new Properties();
    try (InputStream input = Files.newInputStream(passwordFile)) {
      credentials.load(input);
    }
    String password = credentials.getProperty(InfraApplicationAdminClient.JMX_USER);
    assertThat(password).isNotBlank();

    StopMojo stop = new StopMojo();
    ReflectionTestUtils.setField(stop, "buildDirectory", buildDirectory.toFile());
    String stopPassword = ReflectionTestUtils.invokeMethod(stop, "readPassword");
    assertThat(stopPassword).isEqualTo(password);
  }

  @Test
  void stopReportsMissingCredentialsBeforeConnecting() {
    StopMojo stop = new StopMojo();
    ReflectionTestUtils.setField(stop, "buildDirectory", this.directory.toFile());

    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(stop::execute)
            .withMessageContaining("Unable to read JMX credentials");
  }

  @Test
  void stopReportsBlankCredentialsBeforeConnecting() throws Exception {
    Files.writeString(this.directory.resolve(InfraApplicationAdminClient.JMX_PASSWORD_FILE),
            InfraApplicationAdminClient.JMX_USER + "=");
    StopMojo stop = new StopMojo();
    ReflectionTestUtils.setField(stop, "buildDirectory", this.directory.toFile());

    assertThatExceptionOfType(MojoExecutionException.class).isThrownBy(stop::execute)
            .withMessageContaining("No JMX password found");
  }

}
