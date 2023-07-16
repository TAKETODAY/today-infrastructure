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

package cn.taketoday.buildpack.platform.build;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import cn.taketoday.buildpack.platform.docker.DockerApi;
import cn.taketoday.buildpack.platform.docker.TotalProgressEvent;
import cn.taketoday.buildpack.platform.docker.TotalProgressPullListener;
import cn.taketoday.buildpack.platform.docker.TotalProgressPushListener;
import cn.taketoday.buildpack.platform.docker.UpdateListener;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.docker.transport.DockerEngineException;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.io.IOBiConsumer;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Central API for running buildpack operations.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Rafael Ceccone
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Builder {

  private final BuildLog log;

  private final DockerApi docker;

  private final DockerConfiguration dockerConfiguration;

  /**
   * Create a new builder instance.
   */
  public Builder() {
    this(BuildLog.toSystemOut());
  }

  /**
   * Create a new builder instance.
   *
   * @param dockerConfiguration the docker configuration
   * @since 4.0
   */
  public Builder(DockerConfiguration dockerConfiguration) {
    this(BuildLog.toSystemOut(), dockerConfiguration);
  }

  /**
   * Create a new builder instance.
   *
   * @param log a logger used to record output
   */
  public Builder(BuildLog log) {
    this(log, new DockerApi(), null);
  }

  /**
   * Create a new builder instance.
   *
   * @param log a logger used to record output
   * @param dockerConfiguration the docker configuration
   * @since 4.0
   */
  public Builder(BuildLog log, DockerConfiguration dockerConfiguration) {
    this(log, new DockerApi((dockerConfiguration != null) ? dockerConfiguration.getHost() : null),
            dockerConfiguration);
  }

  Builder(BuildLog log, DockerApi docker, DockerConfiguration dockerConfiguration) {
    Assert.notNull(log, "Log must not be null");
    this.log = log;
    this.docker = docker;
    this.dockerConfiguration = dockerConfiguration;
  }

  public void build(BuildRequest request) throws DockerEngineException, IOException {
    Assert.notNull(request, "Request must not be null");
    this.log.start(request);
    String domain = request.getBuilder().getDomain();
    PullPolicy pullPolicy = request.getPullPolicy();
    ImageFetcher imageFetcher = new ImageFetcher(domain, getBuilderAuthHeader(), pullPolicy);
    Image builderImage = imageFetcher.fetchImage(ImageType.BUILDER, request.getBuilder());
    BuilderMetadata builderMetadata = BuilderMetadata.fromImage(builderImage);
    request = withRunImageIfNeeded(request, builderMetadata.getStack());
    Image runImage = imageFetcher.fetchImage(ImageType.RUNNER, request.getRunImage());
    assertStackIdsMatch(runImage, builderImage);
    BuildOwner buildOwner = BuildOwner.fromEnv(builderImage.getConfig().getEnv());
    BuildpackLayersMetadata buildpackLayersMetadata = BuildpackLayersMetadata.fromImage(builderImage);
    Buildpacks buildpacks = getBuildpacks(request, imageFetcher, builderMetadata, buildpackLayersMetadata);
    EphemeralBuilder ephemeralBuilder = new EphemeralBuilder(buildOwner, builderImage, request.getName(),
            builderMetadata, request.getCreator(), request.getEnv(), buildpacks);
    this.docker.image().load(ephemeralBuilder.getArchive(), UpdateListener.none());
    try {
      executeLifecycle(request, ephemeralBuilder);
      tagImage(request.getName(), request.getTags());
      if (request.isPublish()) {
        pushImages(request.getName(), request.getTags());
      }
    }
    finally {
      this.docker.image().remove(ephemeralBuilder.getName(), true);
    }
  }

  private BuildRequest withRunImageIfNeeded(BuildRequest request, BuilderMetadata.Stack builderStack) {
    if (request.getRunImage() != null) {
      return request;
    }
    return request.withRunImage(getRunImageReferenceForStack(builderStack));
  }

  private ImageReference getRunImageReferenceForStack(BuilderMetadata.Stack stack) {
    String name = stack.getRunImage().getImage();
    Assert.state(StringUtils.hasText(name), "Run image must be specified in the builder image stack");
    return ImageReference.of(name).inTaggedOrDigestForm();
  }

  private void assertStackIdsMatch(Image runImage, Image builderImage) {
    StackId runImageStackId = StackId.fromImage(runImage);
    StackId builderImageStackId = StackId.fromImage(builderImage);
    Assert.state(runImageStackId.equals(builderImageStackId), () -> "Run image stack '" + runImageStackId
            + "' does not match builder stack '" + builderImageStackId + "'");
  }

  private Buildpacks getBuildpacks(BuildRequest request, ImageFetcher imageFetcher, BuilderMetadata builderMetadata,
          BuildpackLayersMetadata buildpackLayersMetadata) {
    BuildpackResolverContext resolverContext = new BuilderResolverContext(imageFetcher, builderMetadata,
            buildpackLayersMetadata);
    return BuildpackResolvers.resolveAll(resolverContext, request.getBuildpacks());
  }

  private void executeLifecycle(BuildRequest request, EphemeralBuilder builder) throws IOException {
    ResolvedDockerHost dockerHost = null;
    if (this.dockerConfiguration != null && this.dockerConfiguration.isBindHostToBuilder()) {
      dockerHost = ResolvedDockerHost.from(this.dockerConfiguration.getHost());
    }
    try (Lifecycle lifecycle = new Lifecycle(this.log, this.docker, dockerHost, request, builder)) {
      lifecycle.execute();
    }
  }

  private void tagImage(ImageReference sourceReference, List<ImageReference> tags) throws IOException {
    for (ImageReference tag : tags) {
      this.docker.image().tag(sourceReference, tag);
      this.log.taggedImage(tag);
    }
  }

  private void pushImages(ImageReference name, List<ImageReference> tags) throws IOException {
    pushImage(name);
    for (ImageReference tag : tags) {
      pushImage(tag);
    }
  }

  private void pushImage(ImageReference reference) throws IOException {
    Consumer<TotalProgressEvent> progressConsumer = this.log.pushingImage(reference);
    TotalProgressPushListener listener = new TotalProgressPushListener(progressConsumer);
    this.docker.image().push(reference, listener, getPublishAuthHeader());
    this.log.pushedImage(reference);
  }

  private String getBuilderAuthHeader() {
    return (this.dockerConfiguration != null && this.dockerConfiguration.getBuilderRegistryAuthentication() != null)
           ? this.dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader() : null;
  }

  private String getPublishAuthHeader() {
    return (this.dockerConfiguration != null && this.dockerConfiguration.getPublishRegistryAuthentication() != null)
           ? this.dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader() : null;
  }

  /**
   * Internal utility class used to fetch images.
   */
  private class ImageFetcher {

    private final String domain;

    private final String authHeader;

    private final PullPolicy pullPolicy;

    ImageFetcher(String domain, String authHeader, PullPolicy pullPolicy) {
      this.domain = domain;
      this.authHeader = authHeader;
      this.pullPolicy = pullPolicy;
    }

    Image fetchImage(ImageType type, ImageReference reference) throws IOException {
      Assert.notNull(type, "Type must not be null");
      Assert.notNull(reference, "Reference must not be null");
      Assert.state(this.authHeader == null || reference.getDomain().equals(this.domain),
              () -> String.format("%s '%s' must be pulled from the '%s' authenticated registry",
                      StringUtils.capitalize(type.getDescription()), reference, this.domain));
      if (this.pullPolicy == PullPolicy.ALWAYS) {
        return pullImage(reference, type);
      }
      try {
        return Builder.this.docker.image().inspect(reference);
      }
      catch (DockerEngineException ex) {
        if (this.pullPolicy == PullPolicy.IF_NOT_PRESENT && ex.getStatusCode() == 404) {
          return pullImage(reference, type);
        }
        throw ex;
      }
    }

    private Image pullImage(ImageReference reference, ImageType imageType) throws IOException {
      TotalProgressPullListener listener = new TotalProgressPullListener(
              Builder.this.log.pullingImage(reference, imageType));
      Image image = Builder.this.docker.image().pull(reference, listener, this.authHeader);
      Builder.this.log.pulledImage(image, imageType);
      return image;
    }

  }

  /**
   * {@link BuildpackResolverContext} implementation for the {@link Builder}.
   */
  private class BuilderResolverContext implements BuildpackResolverContext {

    private final ImageFetcher imageFetcher;

    private final BuilderMetadata builderMetadata;

    private final BuildpackLayersMetadata buildpackLayersMetadata;

    BuilderResolverContext(ImageFetcher imageFetcher, BuilderMetadata builderMetadata,
            BuildpackLayersMetadata buildpackLayersMetadata) {
      this.imageFetcher = imageFetcher;
      this.builderMetadata = builderMetadata;
      this.buildpackLayersMetadata = buildpackLayersMetadata;
    }

    @Override
    public List<BuildpackMetadata> getBuildpackMetadata() {
      return this.builderMetadata.getBuildpacks();
    }

    @Override
    public BuildpackLayersMetadata getBuildpackLayersMetadata() {
      return this.buildpackLayersMetadata;
    }

    @Override
    public Image fetchImage(ImageReference reference, ImageType imageType) throws IOException {
      return this.imageFetcher.fetchImage(imageType, reference);
    }

    @Override
    public void exportImageLayers(ImageReference reference, IOBiConsumer<String, Path> exports) throws IOException {
      Builder.this.docker.image().exportLayerFiles(reference, exports);
    }

  }

}
