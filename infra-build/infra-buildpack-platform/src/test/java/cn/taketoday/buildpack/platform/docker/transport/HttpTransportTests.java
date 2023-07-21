/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.docker.transport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpTransport}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class HttpTransportTests {

  @Test
  void createWhenDockerHostVariableIsAddressReturnsRemote() {
    HttpTransport transport = HttpTransport.create(DockerHostConfiguration.forAddress("tcp://192.168.1.0"));
    assertThat(transport).isInstanceOf(RemoteHttpClientTransport.class);
  }

  @Test
  void createWhenDockerHostVariableIsFileReturnsLocal(@TempDir Path tempDir) throws IOException {
    String dummySocketFilePath = Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath().toString();
    HttpTransport transport = HttpTransport.create(DockerHostConfiguration.forAddress(dummySocketFilePath));
    assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
  }

  @Test
  void createWhenDockerHostVariableIsUnixSchemePrefixedFileReturnsLocal(@TempDir Path tempDir) throws IOException {
    String dummySocketFilePath = "unix://" + Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath();
    HttpTransport transport = HttpTransport.create(DockerHostConfiguration.forAddress(dummySocketFilePath));
    assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
  }

}
