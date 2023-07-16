/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.GradleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.DockerHost;

import java.io.File;
import java.util.Base64;

import cn.taketoday.gradle.junit.GradleProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DockerSpec}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerSpecTests {

  private DockerSpec dockerSpec;

  @BeforeEach
  void prepareDockerSpec(@TempDir File temp) {
    this.dockerSpec = GradleProjectBuilder.builder()
            .withProjectDir(temp)
            .build()
            .getObjects()
            .newInstance(DockerSpec.class);
  }

  @Test
  void asDockerConfigurationWithDefaults() {
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
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
    this.dockerSpec.getHost().set("docker.example.com");
    this.dockerSpec.getTlsVerify().set(true);
    this.dockerSpec.getCertPath().set("/tmp/ca-cert");
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
    DockerHost host = dockerConfiguration.getHost();
    assertThat(host.getAddress()).isEqualTo("docker.example.com");
    assertThat(host.isSecure()).isTrue();
    assertThat(host.getCertificatePath()).isEqualTo("/tmp/ca-cert");
    assertThat(dockerConfiguration.isBindHostToBuilder()).isFalse();
    assertThat(this.dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithHostConfigurationNoTlsVerify() {
    this.dockerSpec.getHost().set("docker.example.com");
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
    DockerHost host = dockerConfiguration.getHost();
    assertThat(host.getAddress()).isEqualTo("docker.example.com");
    assertThat(host.isSecure()).isFalse();
    assertThat(host.getCertificatePath()).isNull();
    assertThat(dockerConfiguration.isBindHostToBuilder()).isFalse();
    assertThat(this.dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithBindHostToBuilder() {
    this.dockerSpec.getHost().set("docker.example.com");
    this.dockerSpec.getBindHostToBuilder().set(true);
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
    DockerHost host = dockerConfiguration.getHost();
    assertThat(host.getAddress()).isEqualTo("docker.example.com");
    assertThat(host.isSecure()).isFalse();
    assertThat(host.getCertificatePath()).isNull();
    assertThat(dockerConfiguration.isBindHostToBuilder()).isTrue();
    assertThat(this.dockerSpec.asDockerConfiguration().getBuilderRegistryAuthentication()).isNull();
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"username\" : \"\"")
            .contains("\"password\" : \"\"")
            .contains("\"email\" : \"\"")
            .contains("\"serveraddress\" : \"\"");
  }

  @Test
  void asDockerConfigurationWithUserAuth() {
    this.dockerSpec.builderRegistry((registry) -> {
      registry.getUsername().set("user1");
      registry.getPassword().set("secret1");
      registry.getUrl().set("https://docker1.example.com");
      registry.getEmail().set("docker1@example.com");
    });
    this.dockerSpec.publishRegistry((registry) -> {
      registry.getUsername().set("user2");
      registry.getPassword().set("secret2");
      registry.getUrl().set("https://docker2.example.com");
      registry.getEmail().set("docker2@example.com");
    });
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
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
    assertThat(this.dockerSpec.asDockerConfiguration().getHost()).isNull();
  }

  @Test
  void asDockerConfigurationWithIncompleteBuilderUserAuthFails() {
    this.dockerSpec.builderRegistry((registry) -> {
      registry.getUsername().set("user1");
      registry.getUrl().set("https://docker1.example.com");
      registry.getEmail().set("docker1@example.com");
    });
    assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
            .withMessageContaining("Invalid Docker builder registry configuration");
  }

  @Test
  void asDockerConfigurationWithIncompletePublishUserAuthFails() {
    this.dockerSpec.publishRegistry((registry) -> {
      registry.getUsername().set("user2");
      registry.getUrl().set("https://docker2.example.com");
      registry.getEmail().set("docker2@example.com");
    });
    assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
            .withMessageContaining("Invalid Docker publish registry configuration");
  }

  @Test
  void asDockerConfigurationWithTokenAuth() {
    this.dockerSpec.builderRegistry((registry) -> registry.getToken().set("token1"));
    this.dockerSpec.publishRegistry((registry) -> registry.getToken().set("token2"));
    DockerConfiguration dockerConfiguration = this.dockerSpec.asDockerConfiguration();
    assertThat(decoded(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()))
            .contains("\"identitytoken\" : \"token1\"");
    assertThat(decoded(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()))
            .contains("\"identitytoken\" : \"token2\"");
  }

  @Test
  void asDockerConfigurationWithUserAndTokenAuthFails() {
    this.dockerSpec.builderRegistry((registry) -> {
      registry.getUsername().set("user");
      registry.getPassword().set("secret");
      registry.getToken().set("token");
    });
    assertThatExceptionOfType(GradleException.class).isThrownBy(this.dockerSpec::asDockerConfiguration)
            .withMessageContaining("Invalid Docker builder registry configuration");
  }

  String decoded(String value) {
    return new String(Base64.getDecoder().decode(value));
  }

}
