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

/**
 * Docker host connection options.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class DockerHost {

  private final String address;

  private final boolean secure;

  private final String certificatePath;

  public DockerHost(String address) {
    this(address, false, null);
  }

  public DockerHost(String address, boolean secure, String certificatePath) {
    this.address = address;
    this.secure = secure;
    this.certificatePath = certificatePath;
  }

  public String getAddress() {
    return this.address;
  }

  public boolean isSecure() {
    return this.secure;
  }

  public String getCertificatePath() {
    return this.certificatePath;
  }

}
