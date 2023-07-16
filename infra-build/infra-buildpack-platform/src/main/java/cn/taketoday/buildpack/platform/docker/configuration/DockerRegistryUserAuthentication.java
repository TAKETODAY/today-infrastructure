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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Docker registry authentication configuration using user credentials.
 *
 * @author Scott Frederick
 */
class DockerRegistryUserAuthentication extends JsonEncodedDockerRegistryAuthentication {

  @JsonProperty
  private final String username;

  @JsonProperty
  private final String password;

  @JsonProperty("serveraddress")
  private final String url;

  @JsonProperty
  private final String email;

  DockerRegistryUserAuthentication(String username, String password, String url, String email) {
    this.username = username;
    this.password = password;
    this.url = url;
    this.email = email;
    createAuthHeader();
  }

  String getUsername() {
    return this.username;
  }

  String getPassword() {
    return this.password;
  }

  String getUrl() {
    return this.url;
  }

  String getEmail() {
    return this.email;
  }

}
