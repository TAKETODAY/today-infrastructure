/*
 * Copyright 2012 - 2023 the original author or authors.
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

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;

/**
 * Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Docker {

  private String host;

  private boolean tlsVerify;

  private String certPath;

  private boolean bindHostToBuilder;

  private DockerRegistry builderRegistry;

  private DockerRegistry publishRegistry;

  /**
   * The host address of the Docker daemon.
   *
   * @return the Docker host
   */
  public String getHost() {
    return this.host;
  }

  void setHost(String host) {
    this.host = host;
  }

  /**
   * Whether the Docker daemon requires TLS communication.
   *
   * @return {@code true} to enable TLS
   */
  public boolean isTlsVerify() {
    return this.tlsVerify;
  }

  void setTlsVerify(boolean tlsVerify) {
    this.tlsVerify = tlsVerify;
  }

  /**
   * The path to TLS certificate and key files required for TLS communication with the
   * Docker daemon.
   *
   * @return the TLS certificate path
   */
  public String getCertPath() {
    return this.certPath;
  }

  void setCertPath(String certPath) {
    this.certPath = certPath;
  }

  /**
   * Whether to use the configured Docker host in the builder container.
   *
   * @return {@code true} to use the configured Docker host in the builder container
   */
  public boolean isBindHostToBuilder() {
    return this.bindHostToBuilder;
  }

  void setBindHostToBuilder(boolean bindHostToBuilder) {
    this.bindHostToBuilder = bindHostToBuilder;
  }

  /**
   * Configuration of the Docker registry where builder and run images are stored.
   *
   * @return the registry configuration
   */
  DockerRegistry getBuilderRegistry() {
    return this.builderRegistry;
  }

  /**
   * Sets the {@link DockerRegistry} that configures authentication to the builder
   * registry.
   *
   * @param builderRegistry the registry configuration
   */
  void setBuilderRegistry(DockerRegistry builderRegistry) {
    this.builderRegistry = builderRegistry;
  }

  /**
   * Configuration of the Docker registry where the generated image will be published.
   *
   * @return the registry configuration
   */
  DockerRegistry getPublishRegistry() {
    return this.publishRegistry;
  }

  /**
   * Sets the {@link DockerRegistry} that configures authentication to the publishing
   * registry.
   *
   * @param builderRegistry the registry configuration
   */
  void setPublishRegistry(DockerRegistry builderRegistry) {
    this.publishRegistry = builderRegistry;
  }

  /**
   * Returns this configuration as a {@link DockerConfiguration} instance. This method
   * should only be called when the configuration is complete and will no longer be
   * changed.
   *
   * @return the Docker configuration
   */
  DockerConfiguration asDockerConfiguration() {
    DockerConfiguration dockerConfiguration = new DockerConfiguration();
    dockerConfiguration = customizeHost(dockerConfiguration);
    dockerConfiguration = dockerConfiguration.withBindHostToBuilder(this.bindHostToBuilder);
    dockerConfiguration = customizeBuilderAuthentication(dockerConfiguration);
    dockerConfiguration = customizePublishAuthentication(dockerConfiguration);
    return dockerConfiguration;
  }

  private DockerConfiguration customizeHost(DockerConfiguration dockerConfiguration) {
    if (this.host != null) {
      return dockerConfiguration.withHost(this.host, this.tlsVerify, this.certPath);
    }
    return dockerConfiguration;
  }

  private DockerConfiguration customizeBuilderAuthentication(DockerConfiguration dockerConfiguration) {
    if (this.builderRegistry == null || this.builderRegistry.isEmpty()) {
      return dockerConfiguration;
    }
    if (this.builderRegistry.hasTokenAuth() && !this.builderRegistry.hasUserAuth()) {
      return dockerConfiguration.withBuilderRegistryTokenAuthentication(this.builderRegistry.getToken());
    }
    if (this.builderRegistry.hasUserAuth() && !this.builderRegistry.hasTokenAuth()) {
      return dockerConfiguration.withBuilderRegistryUserAuthentication(this.builderRegistry.getUsername(),
              this.builderRegistry.getPassword(), this.builderRegistry.getUrl(), this.builderRegistry.getEmail());
    }
    throw new IllegalArgumentException(
            "Invalid Docker builder registry configuration, either token or username/password must be provided");
  }

  private DockerConfiguration customizePublishAuthentication(DockerConfiguration dockerConfiguration) {
    if (this.publishRegistry == null || this.publishRegistry.isEmpty()) {
      return dockerConfiguration.withEmptyPublishRegistryAuthentication();
    }
    if (this.publishRegistry.hasTokenAuth() && !this.publishRegistry.hasUserAuth()) {
      return dockerConfiguration.withPublishRegistryTokenAuthentication(this.publishRegistry.getToken());
    }
    if (this.publishRegistry.hasUserAuth() && !this.publishRegistry.hasTokenAuth()) {
      return dockerConfiguration.withPublishRegistryUserAuthentication(this.publishRegistry.getUsername(),
              this.publishRegistry.getPassword(), this.publishRegistry.getUrl(), this.publishRegistry.getEmail());
    }
    throw new IllegalArgumentException(
            "Invalid Docker publish registry configuration, either token or username/password must be provided");
  }

  /**
   * Encapsulates Docker registry authentication configuration options.
   */
  public static class DockerRegistry {

    private String username;

    private String password;

    private String url;

    private String email;

    private String token;

    public DockerRegistry() {
    }

    public DockerRegistry(String username, String password, String url, String email) {
      this.username = username;
      this.password = password;
      this.url = url;
      this.email = email;
    }

    public DockerRegistry(String token) {
      this.token = token;
    }

    /**
     * The username that will be used for user authentication to the registry.
     *
     * @return the username
     */
    public String getUsername() {
      return this.username;
    }

    void setUsername(String username) {
      this.username = username;
    }

    /**
     * The password that will be used for user authentication to the registry.
     *
     * @return the password
     */
    public String getPassword() {
      return this.password;
    }

    void setPassword(String password) {
      this.password = password;
    }

    /**
     * The email address that will be used for user authentication to the registry.
     *
     * @return the email address
     */
    public String getEmail() {
      return this.email;
    }

    void setEmail(String email) {
      this.email = email;
    }

    /**
     * The URL of the registry.
     *
     * @return the registry URL
     */
    String getUrl() {
      return this.url;
    }

    void setUrl(String url) {
      this.url = url;
    }

    /**
     * The token that will be used for token authentication to the registry.
     *
     * @return the authentication token
     */
    public String getToken() {
      return this.token;
    }

    void setToken(String token) {
      this.token = token;
    }

    boolean isEmpty() {
      return this.username == null && this.password == null && this.url == null && this.email == null
              && this.token == null;
    }

    boolean hasTokenAuth() {
      return this.token != null;
    }

    boolean hasUserAuth() {
      return this.username != null && this.password != null;
    }

  }

}
