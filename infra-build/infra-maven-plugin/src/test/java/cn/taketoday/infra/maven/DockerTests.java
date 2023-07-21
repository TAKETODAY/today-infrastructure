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

package cn.taketoday.infra.maven;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Docker}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerTests {

  @Test
  void asDockerConfigurationWithDefaults() {
    Docker docker = new Docker();
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    assertThat(dockerConfiguration.getHost()).isNull();
    assertThat(dockerConfiguration.getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithHostConfiguration() {
    Docker docker = new Docker();
    docker.setHost("docker.example.com");
    docker.setTlsVerify(true);
    docker.setCertPath("/tmp/ca-cert");
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    DockerHostConfiguration host = dockerConfiguration.getHost();
    assertThat(host.getAddress()).isEqualTo("docker.example.com");
    assertThat(host.isSecure()).isTrue();
    assertThat(host.getCertificatePath()).isEqualTo("/tmp/ca-cert");
    assertThat(host.getContext()).isNull();
    assertThat(dockerConfiguration.isBindHostToBuilder()).isFalse();
    assertThat(docker.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithContextConfiguration() {
    Docker docker = new Docker();
    docker.setContext("test-context");
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    DockerHostConfiguration host = dockerConfiguration.getHost();
    assertThat(host.getContext()).isEqualTo("test-context");
    assertThat(host.getAddress()).isNull();
    assertThat(host.isSecure()).isFalse();
    assertThat(host.getCertificatePath()).isNull();
    assertThat(dockerConfiguration.isBindHostToBuilder()).isFalse();
    assertThat(docker.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithHostAndContextFails() {
    Docker docker = new Docker();
    docker.setContext("test-context");
    docker.setHost("docker.example.com");
    assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
            .withMessageContaining("Invalid Docker configuration");
  }

  @Test
  void asDockerConfigurationWithBindHostToBuilder() {
    Docker docker = new Docker();
    docker.setHost("docker.example.com");
    docker.setTlsVerify(true);
    docker.setCertPath("/tmp/ca-cert");
    docker.setBindHostToBuilder(true);
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    DockerHostConfiguration host = dockerConfiguration.getHost();
    assertThat(host.getAddress()).isEqualTo("docker.example.com");
    assertThat(host.isSecure()).isTrue();
    assertThat(host.getCertificatePath()).isEqualTo("/tmp/ca-cert");
    assertThat(dockerConfiguration.isBindHostToBuilder()).isTrue();
    assertThat(docker.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithUserAuth() {
    Docker docker = new Docker();
    docker.setBuilderRegistry(
            new Docker.DockerRegistry("user1", "secret1", "https://docker1.example.com", "docker1@example.com"));
    docker.setPublishRegistry(
            new Docker.DockerRegistry("user2", "secret2", "https://docker2.example.com", "docker2@example.com"));
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"user1\"")
            .contains("\"password\" : \"secret1\"")
            .contains("\"email\" : \"docker1@example.com\"")
            .contains("\"serveraddress\" : \"https://docker1.example.com\"");
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"user2\"")
            .contains("\"password\" : \"secret2\"")
            .contains("\"email\" : \"docker2@example.com\"")
            .contains("\"serveraddress\" : \"https://docker2.example.com\"");
  }

  @Test
  void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
    Docker docker = new Docker();
    docker.setBuilderRegistry(
            new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
    assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
            .withMessageContaining("Invalid Docker builder registry configuration");
  }

  @Test
  void asDockerConfigurationWithIncompletePublishUserAuthFails() {
    Docker docker = new Docker();
    docker.setPublishRegistry(
            new Docker.DockerRegistry("user", null, "https://docker.example.com", "docker@example.com"));
    assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
            .withMessageContaining("Invalid Docker publish registry configuration");
  }

  @Test
  void asDockerConfigurationWithTokenAuth() {
    Docker docker = new Docker();
    docker.setBuilderRegistry(new Docker.DockerRegistry("token1"));
    docker.setPublishRegistry(new Docker.DockerRegistry("token2"));
    DockerConfiguration dockerConfiguration = docker.asDockerConfiguration();
    assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
            .contains("\"identitytoken\" : \"token1\"");
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"identitytoken\" : \"token2\"");
  }

  @Test
  void asDockerConfigurationWithUserAndTokenAuthFails() {
    Docker.DockerRegistry dockerRegistry = new Docker.DockerRegistry();
    dockerRegistry.setUsername("user");
    dockerRegistry.setPassword("secret");
    dockerRegistry.setToken("token");
    Docker docker = new Docker();
    docker.setBuilderRegistry(dockerRegistry);
    assertThatIllegalArgumentException().isThrownBy(docker::asDockerConfiguration)
            .withMessageContaining("Invalid Docker builder registry configuration");
  }

  String decoded(String value) {
    return new String(Base64.getDecoder().decode(value));
  }

}
