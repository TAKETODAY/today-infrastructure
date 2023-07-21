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
import java.nio.file.Paths;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalHttpClientTransport}
 *
 * @author Scott Frederick
 */
class LocalHttpClientTransportTests {

  @Test
  void createWhenDockerHostIsFileReturnsTransport(@TempDir Path tempDir) throws IOException {
    String socketFilePath = Files.createTempFile(tempDir, "remote-transport", null).toAbsolutePath().toString();
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration.forAddress(socketFilePath));
    LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
    assertThat(transport).isNotNull();
    assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
  }

  @Test
  void createWhenDockerHostIsFileThatDoesNotExistReturnsTransport(@TempDir Path tempDir) {
    String socketFilePath = Paths.get(tempDir.toString(), "dummy").toAbsolutePath().toString();
    ResolvedDockerHost dockerHost = ResolvedDockerHost.from(DockerHostConfiguration.forAddress(socketFilePath));
    LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
    assertThat(transport).isNotNull();
    assertThat(transport.getHost().toHostString()).isEqualTo(socketFilePath);
  }

  @Test
  void createWhenDockerHostIsAddressReturnsTransport() {
    ResolvedDockerHost dockerHost = ResolvedDockerHost
            .from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376"));
    LocalHttpClientTransport transport = LocalHttpClientTransport.create(dockerHost);
    assertThat(transport).isNotNull();
    assertThat(transport.getHost().toHostString()).isEqualTo("tcp://192.168.1.2:2376");
  }

}
