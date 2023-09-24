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

import org.apache.maven.artifact.Artifact;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.buildpack.platform.build.BuildRequest;
import cn.taketoday.buildpack.platform.build.BuildpackReference;
import cn.taketoday.buildpack.platform.build.PullPolicy;
import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ImageName;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.io.Owner;
import cn.taketoday.buildpack.platform.io.TarArchive;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Image configuration options.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 * @author Julian Liebig
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Image {

  String name;

  String builder;

  String runImage;

  Map<String, String> env;

  Boolean cleanCache;

  boolean verboseLogging;

  PullPolicy pullPolicy;

  Boolean publish;

  List<String> buildpacks;

  List<String> bindings;

  String network;

  List<String> tags;

  CacheInfo buildWorkspace;

  CacheInfo buildCache;

  CacheInfo launchCache;

  String createdDate;

  String applicationDirectory;

  List<String> securityOptions;

  /**
   * The name of the created image.
   *
   * @return the image name
   */
  public String getName() {
    return this.name;
  }

  void setName(String name) {
    this.name = name;
  }

  /**
   * The name of the builder image to use to create the image.
   *
   * @return the builder image name
   */
  public String getBuilder() {
    return this.builder;
  }

  void setBuilder(String builder) {
    this.builder = builder;
  }

  /**
   * The name of the run image to use to create the image.
   *
   * @return the builder image name
   */
  public String getRunImage() {
    return this.runImage;
  }

  void setRunImage(String runImage) {
    this.runImage = runImage;
  }

  /**
   * Environment properties that should be passed to the builder.
   *
   * @return the environment properties
   */
  public Map<String, String> getEnv() {
    return this.env;
  }

  /**
   * If the cache should be cleaned before building.
   *
   * @return {@code true} if the cache should be cleaned
   */
  public Boolean getCleanCache() {
    return this.cleanCache;
  }

  void setCleanCache(Boolean cleanCache) {
    this.cleanCache = cleanCache;
  }

  /**
   * If verbose logging is required.
   *
   * @return {@code true} for verbose logging
   */
  public boolean isVerboseLogging() {
    return this.verboseLogging;
  }

  /**
   * If images should be pulled from a remote repository during image build.
   *
   * @return the pull policy
   */
  public PullPolicy getPullPolicy() {
    return this.pullPolicy;
  }

  void setPullPolicy(PullPolicy pullPolicy) {
    this.pullPolicy = pullPolicy;
  }

  /**
   * If the built image should be pushed to a registry.
   *
   * @return {@code true} if the image should be published
   */
  public Boolean getPublish() {
    return this.publish;
  }

  void setPublish(Boolean publish) {
    this.publish = publish;
  }

  /**
   * Returns the network the build container will connect to.
   *
   * @return the network
   */
  public String getNetwork() {
    return this.network;
  }

  public void setNetwork(String network) {
    this.network = network;
  }

  /**
   * Returns the created date for the image.
   *
   * @return the created date
   */
  public String getCreatedDate() {
    return this.createdDate;
  }

  public void setCreatedDate(String createdDate) {
    this.createdDate = createdDate;
  }

  /**
   * Returns the application content directory for the image.
   *
   * @return the application directory
   */
  public String getApplicationDirectory() {
    return this.applicationDirectory;
  }

  public void setApplicationDirectory(String applicationDirectory) {
    this.applicationDirectory = applicationDirectory;
  }

  BuildRequest getBuildRequest(Artifact artifact, Function<Owner, TarArchive> applicationContent) {
    return customize(BuildRequest.of(getOrDeduceName(artifact), applicationContent));
  }

  private ImageReference getOrDeduceName(Artifact artifact) {
    if (StringUtils.hasText(this.name)) {
      return ImageReference.of(this.name);
    }
    ImageName imageName = ImageName.of(artifact.getArtifactId());
    return ImageReference.of(imageName, artifact.getVersion());
  }

  private BuildRequest customize(BuildRequest request) {
    if (StringUtils.hasText(this.builder)) {
      request = request.withBuilder(ImageReference.of(this.builder));
    }
    if (StringUtils.hasText(this.runImage)) {
      request = request.withRunImage(ImageReference.of(this.runImage));
    }
    if (this.env != null && !this.env.isEmpty()) {
      request = request.withEnv(this.env);
    }
    if (this.cleanCache != null) {
      request = request.withCleanCache(this.cleanCache);
    }
    request = request.withVerboseLogging(this.verboseLogging);
    if (this.pullPolicy != null) {
      request = request.withPullPolicy(this.pullPolicy);
    }
    if (this.publish != null) {
      request = request.withPublish(this.publish);
    }
    if (!CollectionUtils.isEmpty(this.buildpacks)) {
      request = request.withBuildpacks(this.buildpacks.stream().map(BuildpackReference::of).toList());
    }
    if (!CollectionUtils.isEmpty(this.bindings)) {
      request = request.withBindings(this.bindings.stream().map(Binding::of).toList());
    }
    request = request.withNetwork(this.network);
    if (!CollectionUtils.isEmpty(this.tags)) {
      request = request.withTags(this.tags.stream().map(ImageReference::of).toList());
    }
    if (this.buildWorkspace != null) {
      request = request.withBuildWorkspace(this.buildWorkspace.asCache());
    }
    if (this.buildCache != null) {
      request = request.withBuildCache(this.buildCache.asCache());
    }
    if (this.launchCache != null) {
      request = request.withLaunchCache(this.launchCache.asCache());
    }
    if (StringUtils.hasText(this.createdDate)) {
      request = request.withCreatedDate(this.createdDate);
    }
    if (StringUtils.hasText(this.applicationDirectory)) {
      request = request.withApplicationDirectory(this.applicationDirectory);
    }
    if (this.securityOptions != null) {
      request = request.withSecurityOptions(this.securityOptions);
    }
    return request;
  }

}
