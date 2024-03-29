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

import cn.taketoday.lang.Assert;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @since 4.0
 */
public final class DockerConfiguration {

  private final DockerHostConfiguration host;

  private final DockerRegistryAuthentication builderAuthentication;

  private final DockerRegistryAuthentication publishAuthentication;

  private final boolean bindHostToBuilder;

  public DockerConfiguration() {
    this(null, null, null, false);
  }

  private DockerConfiguration(DockerHostConfiguration host, DockerRegistryAuthentication builderAuthentication,
          DockerRegistryAuthentication publishAuthentication, boolean bindHostToBuilder) {
    this.host = host;
    this.builderAuthentication = builderAuthentication;
    this.publishAuthentication = publishAuthentication;
    this.bindHostToBuilder = bindHostToBuilder;
  }

  public DockerHostConfiguration getHost() {
    return this.host;
  }

  public boolean isBindHostToBuilder() {
    return this.bindHostToBuilder;
  }

  public DockerRegistryAuthentication getBuilderRegistryAuthentication() {
    return this.builderAuthentication;
  }

  public DockerRegistryAuthentication getPublishRegistryAuthentication() {
    return this.publishAuthentication;
  }

  public DockerConfiguration withHost(String address, boolean secure, String certificatePath) {
    Assert.notNull(address, "Address is required");
    return new DockerConfiguration(DockerHostConfiguration.forAddress(address, secure, certificatePath),
            this.builderAuthentication, this.publishAuthentication, this.bindHostToBuilder);
  }

  public DockerConfiguration withContext(String context) {
    Assert.notNull(context, "Context is required");
    return new DockerConfiguration(DockerHostConfiguration.forContext(context), this.builderAuthentication,
            this.publishAuthentication, this.bindHostToBuilder);
  }

  public DockerConfiguration withBindHostToBuilder(boolean bindHostToBuilder) {
    return new DockerConfiguration(this.host, this.builderAuthentication, this.publishAuthentication,
            bindHostToBuilder);
  }

  public DockerConfiguration withBuilderRegistryTokenAuthentication(String token) {
    Assert.notNull(token, "Token is required");
    return new DockerConfiguration(this.host, new DockerRegistryTokenAuthentication(token),
            this.publishAuthentication, this.bindHostToBuilder);
  }

  public DockerConfiguration withBuilderRegistryUserAuthentication(String username, String password, String url,
          String email) {
    Assert.notNull(username, "Username is required");
    Assert.notNull(password, "Password is required");
    return new DockerConfiguration(this.host, new DockerRegistryUserAuthentication(username, password, url, email),
            this.publishAuthentication, this.bindHostToBuilder);
  }

  public DockerConfiguration withPublishRegistryTokenAuthentication(String token) {
    Assert.notNull(token, "Token is required");
    return new DockerConfiguration(this.host, this.builderAuthentication,
            new DockerRegistryTokenAuthentication(token), this.bindHostToBuilder);
  }

  public DockerConfiguration withPublishRegistryUserAuthentication(String username, String password, String url,
          String email) {
    Assert.notNull(username, "Username is required");
    Assert.notNull(password, "Password is required");
    return new DockerConfiguration(this.host, this.builderAuthentication,
            new DockerRegistryUserAuthentication(username, password, url, email), this.bindHostToBuilder);
  }

  public DockerConfiguration withEmptyPublishRegistryAuthentication() {
    return new DockerConfiguration(this.host, this.builderAuthentication,
            new DockerRegistryUserAuthentication("", "", "", ""), this.bindHostToBuilder);
  }

  public static class DockerHostConfiguration {

    private final String address;

    private final String context;

    private final boolean secure;

    private final String certificatePath;

    public DockerHostConfiguration(String address, String context, boolean secure, String certificatePath) {
      this.address = address;
      this.context = context;
      this.secure = secure;
      this.certificatePath = certificatePath;
    }

    public String getAddress() {
      return this.address;
    }

    public String getContext() {
      return this.context;
    }

    public boolean isSecure() {
      return this.secure;
    }

    public String getCertificatePath() {
      return this.certificatePath;
    }

    public static DockerHostConfiguration forAddress(String address) {
      return new DockerHostConfiguration(address, null, false, null);
    }

    public static DockerHostConfiguration forAddress(String address, boolean secure, String certificatePath) {
      return new DockerHostConfiguration(address, null, secure, certificatePath);
    }

    static DockerHostConfiguration forContext(String context) {
      return new DockerHostConfiguration(null, context, false, null);
    }

  }

}
