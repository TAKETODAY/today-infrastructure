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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;

import javax.inject.Inject;

/**
 * Encapsulates Docker configuration options.
 *
 * @author Wei Jiang
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class DockerSpec {

  private final DockerRegistrySpec builderRegistry;

  private final DockerRegistrySpec publishRegistry;

  @Inject
  public DockerSpec(ObjectFactory objects) {
    this.builderRegistry = objects.newInstance(DockerRegistrySpec.class);
    this.publishRegistry = objects.newInstance(DockerRegistrySpec.class);
    getBindHostToBuilder().convention(false);
    getTlsVerify().convention(false);
  }

  DockerSpec(DockerRegistrySpec builderRegistry, DockerRegistrySpec publishRegistry) {
    this.builderRegistry = builderRegistry;
    this.publishRegistry = publishRegistry;
  }

  @Input
  @Optional
  public abstract Property<String> getHost();

  @Input
  @Optional
  public abstract Property<Boolean> getTlsVerify();

  @Input
  @Optional
  public abstract Property<String> getCertPath();

  @Input
  @Optional
  public abstract Property<Boolean> getBindHostToBuilder();

  /**
   * Returns the {@link DockerRegistrySpec} that configures authentication to the
   * builder registry.
   *
   * @return the registry spec
   */
  @Nested
  public DockerRegistrySpec getBuilderRegistry() {
    return this.builderRegistry;
  }

  /**
   * Customizes the {@link DockerRegistrySpec} that configures authentication to the
   * builder registry.
   *
   * @param action the action to apply
   */
  public void builderRegistry(Action<DockerRegistrySpec> action) {
    action.execute(this.builderRegistry);
  }

  /**
   * Returns the {@link DockerRegistrySpec} that configures authentication to the
   * publishing registry.
   *
   * @return the registry spec
   */
  @Nested
  public DockerRegistrySpec getPublishRegistry() {
    return this.publishRegistry;
  }

  /**
   * Customizes the {@link DockerRegistrySpec} that configures authentication to the
   * publishing registry.
   *
   * @param action the action to apply
   */
  public void publishRegistry(Action<DockerRegistrySpec> action) {
    action.execute(this.publishRegistry);
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
    dockerConfiguration = dockerConfiguration.withBindHostToBuilder(getBindHostToBuilder().get());
    dockerConfiguration = customizeBuilderAuthentication(dockerConfiguration);
    dockerConfiguration = customizePublishAuthentication(dockerConfiguration);
    return dockerConfiguration;
  }

  private DockerConfiguration customizeHost(DockerConfiguration dockerConfiguration) {
    String host = getHost().getOrNull();
    if (host != null) {
      return dockerConfiguration.withHost(host, getTlsVerify().get(), getCertPath().getOrNull());
    }
    return dockerConfiguration;
  }

  private DockerConfiguration customizeBuilderAuthentication(DockerConfiguration dockerConfiguration) {
    if (this.builderRegistry == null || this.builderRegistry.hasEmptyAuth()) {
      return dockerConfiguration;
    }
    if (this.builderRegistry.hasTokenAuth() && !this.builderRegistry.hasUserAuth()) {
      return dockerConfiguration.withBuilderRegistryTokenAuthentication(this.builderRegistry.getToken().get());
    }
    if (this.builderRegistry.hasUserAuth() && !this.builderRegistry.hasTokenAuth()) {
      return dockerConfiguration.withBuilderRegistryUserAuthentication(this.builderRegistry.getUsername().get(),
              this.builderRegistry.getPassword().get(), this.builderRegistry.getUrl().getOrNull(),
              this.builderRegistry.getEmail().getOrNull());
    }
    throw new GradleException(
            "Invalid Docker builder registry configuration, either token or username/password must be provided");
  }

  private DockerConfiguration customizePublishAuthentication(DockerConfiguration dockerConfiguration) {
    if (this.publishRegistry == null || this.publishRegistry.hasEmptyAuth()) {
      return dockerConfiguration.withEmptyPublishRegistryAuthentication();
    }
    if (this.publishRegistry.hasTokenAuth() && !this.publishRegistry.hasUserAuth()) {
      return dockerConfiguration.withPublishRegistryTokenAuthentication(this.publishRegistry.getToken().get());
    }
    if (this.publishRegistry.hasUserAuth() && !this.publishRegistry.hasTokenAuth()) {
      return dockerConfiguration.withPublishRegistryUserAuthentication(this.publishRegistry.getUsername().get(),
              this.publishRegistry.getPassword().get(), this.publishRegistry.getUrl().getOrNull(),
              this.publishRegistry.getEmail().getOrNull());
    }
    throw new GradleException(
            "Invalid Docker publish registry configuration, either token or username/password must be provided");
  }

  /**
   * Encapsulates Docker registry authentication configuration options.
   */
  public abstract static class DockerRegistrySpec {

    /**
     * Returns the username to use when authenticating to the Docker registry.
     *
     * @return the registry username
     */
    @Input
    @Optional
    public abstract Property<String> getUsername();

    /**
     * Returns the password to use when authenticating to the Docker registry.
     *
     * @return the registry password
     */
    @Input
    @Optional
    public abstract Property<String> getPassword();

    /**
     * Returns the Docker registry URL.
     *
     * @return the registry URL
     */
    @Input
    @Optional
    public abstract Property<String> getUrl();

    /**
     * Returns the email address associated with the Docker registry username.
     *
     * @return the registry email address
     */
    @Input
    @Optional
    public abstract Property<String> getEmail();

    /**
     * Returns the identity token to use when authenticating to the Docker registry.
     *
     * @return the registry identity token
     */
    @Input
    @Optional
    public abstract Property<String> getToken();

    boolean hasEmptyAuth() {
      return nonePresent(getUsername(), getPassword(), getUrl(), getEmail(), getToken());
    }

    private boolean nonePresent(Property<?>... properties) {
      for (Property<?> property : properties) {
        if (property.isPresent()) {
          return false;
        }
      }
      return true;
    }

    boolean hasUserAuth() {
      return allPresent(getUsername(), getPassword());
    }

    private boolean allPresent(Property<?>... properties) {
      for (Property<?> property : properties) {
        if (!property.isPresent()) {
          return false;
        }
      }
      return true;
    }

    boolean hasTokenAuth() {
      return getToken().isPresent();
    }

  }

}
