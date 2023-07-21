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

package cn.taketoday.buildpack.platform.docker.configuration;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfigurationMetadata.DockerContext;
import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DockerConfigurationMetadata}.
 *
 * @author Scott Frederick
 */
class DockerConfigurationMetadataTests extends AbstractJsonTests {

  private final Map<String, String> environment = new LinkedHashMap<>();

  @Test
  void configWithContextIsRead() throws Exception {
    this.environment.put("DOCKER_CONFIG", pathToResource("with-context/config.json"));
    DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
    assertThat(config.getConfiguration().getCurrentContext()).isEqualTo("test-context");
    assertThat(config.getContext().getDockerHost()).isEqualTo("unix:///home/user/.docker/docker.sock");
    assertThat(config.getContext().isTlsVerify()).isFalse();
    assertThat(config.getContext().getTlsPath()).isNull();
  }

  @Test
  void configWithoutContextIsRead() throws Exception {
    this.environment.put("DOCKER_CONFIG", pathToResource("without-context/config.json"));
    DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
    assertThat(config.getConfiguration().getCurrentContext()).isNull();
    assertThat(config.getContext().getDockerHost()).isNull();
    assertThat(config.getContext().isTlsVerify()).isFalse();
    assertThat(config.getContext().getTlsPath()).isNull();
  }

  @Test
  void configWithDefaultContextIsRead() throws Exception {
    this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
    DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
    assertThat(config.getConfiguration().getCurrentContext()).isEqualTo("default");
    assertThat(config.getContext().getDockerHost()).isNull();
    assertThat(config.getContext().isTlsVerify()).isFalse();
    assertThat(config.getContext().getTlsPath()).isNull();
  }

  @Test
  void configIsReadWithProvidedContext() throws Exception {
    this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
    DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
    DockerContext context = config.forContext("test-context");
    assertThat(context.getDockerHost()).isEqualTo("unix:///home/user/.docker/docker.sock");
    assertThat(context.isTlsVerify()).isTrue();
    assertThat(context.getTlsPath()).matches("^.*/with-default-context/contexts/tls/[a-zA-z0-9]*/docker$");
  }

  @Test
  void invalidContextThrowsException() throws Exception {
    this.environment.put("DOCKER_CONFIG", pathToResource("with-default-context/config.json"));
    assertThatIllegalArgumentException()
            .isThrownBy(() -> DockerConfigurationMetadata.from(this.environment::get).forContext("invalid-context"))
            .withMessageContaining("Docker context 'invalid-context' does not exist");
  }

  @Test
  void configIsEmptyWhenConfigFileDoesNotExist() {
    this.environment.put("DOCKER_CONFIG", "docker-config-dummy-path");
    DockerConfigurationMetadata config = DockerConfigurationMetadata.from(this.environment::get);
    assertThat(config.getConfiguration().getCurrentContext()).isNull();
    assertThat(config.getContext().getDockerHost()).isNull();
    assertThat(config.getContext().isTlsVerify()).isFalse();
  }

  private String pathToResource(String resource) throws URISyntaxException {
    URL url = getClass().getResource(resource);
    return Paths.get(url.toURI()).getParent().toAbsolutePath().toString();
  }

}
