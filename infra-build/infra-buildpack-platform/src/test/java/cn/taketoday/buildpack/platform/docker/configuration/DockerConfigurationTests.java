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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerConfiguration}.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 */
class DockerConfigurationTests {

  @Test
  void createDockerConfigurationWithDefaults() {
    DockerConfiguration configuration = new DockerConfiguration();
    assertThat(configuration.getBuilderRegistryAuthentication()).isNull();
  }

  @Test
  void createDockerConfigurationWithUserAuth() {
    DockerConfiguration configuration = new DockerConfiguration().withBuilderRegistryUserAuthentication("user",
            "secret", "https://docker.example.com", "docker@example.com");
    DockerRegistryAuthentication auth = configuration.getBuilderRegistryAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth).isInstanceOf(DockerRegistryUserAuthentication.class);
    DockerRegistryUserAuthentication userAuth = (DockerRegistryUserAuthentication) auth;
    assertThat(userAuth.getUrl()).isEqualTo("https://docker.example.com");
    assertThat(userAuth.getUsername()).isEqualTo("user");
    assertThat(userAuth.getPassword()).isEqualTo("secret");
    assertThat(userAuth.getEmail()).isEqualTo("docker@example.com");
  }

  @Test
  void createDockerConfigurationWithTokenAuth() {
    DockerConfiguration configuration = new DockerConfiguration().withBuilderRegistryTokenAuthentication("token");
    DockerRegistryAuthentication auth = configuration.getBuilderRegistryAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth).isInstanceOf(DockerRegistryTokenAuthentication.class);
    DockerRegistryTokenAuthentication tokenAuth = (DockerRegistryTokenAuthentication) auth;
    assertThat(tokenAuth.getToken()).isEqualTo("token");
  }

}
