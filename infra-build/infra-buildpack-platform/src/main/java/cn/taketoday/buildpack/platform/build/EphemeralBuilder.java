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
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageArchive;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.Owner;

/**
 * A short-lived builder that is created for each {@link Lifecycle} run.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class EphemeralBuilder {

  static final String BUILDER_FOR_LABEL_NAME = "cn.taketoday.framework.builderFor";

  private final BuildOwner buildOwner;

  private final BuilderMetadata builderMetadata;

  private final ImageArchive archive;

  private final Creator creator;

  /**
   * Create a new {@link EphemeralBuilder} instance.
   *
   * @param buildOwner the build owner
   * @param builderImage the base builder image
   * @param targetImage the image being built
   * @param builderMetadata the builder metadata
   * @param creator the builder creator
   * @param env the builder env
   * @param buildpacks an optional set of buildpacks to apply
   * @throws IOException on IO error
   */
  EphemeralBuilder(BuildOwner buildOwner, Image builderImage, ImageReference targetImage,
          BuilderMetadata builderMetadata, Creator creator, Map<String, String> env, Buildpacks buildpacks)
          throws IOException {
    ImageReference name = ImageReference.random("pack.local/builder/").inTaggedForm();
    this.buildOwner = buildOwner;
    this.creator = creator;
    this.builderMetadata = builderMetadata.copy(this::updateMetadata);
    this.archive = ImageArchive.from(builderImage, (update) -> {
      update.withUpdatedConfig(this.builderMetadata::attachTo);
      update.withUpdatedConfig((config) -> config.withLabel(BUILDER_FOR_LABEL_NAME, targetImage.toString()));
      update.withTag(name);
      if (env != null && !env.isEmpty()) {
        update.withNewLayer(getEnvLayer(env));
      }
      if (buildpacks != null) {
        buildpacks.apply(update::withNewLayer);
      }
    });
  }

  private void updateMetadata(BuilderMetadata.Update update) {
    update.withCreatedBy(this.creator.getName(), this.creator.getVersion());
  }

  private Layer getEnvLayer(Map<String, String> env) throws IOException {
    return Layer.of((layout) -> {
      for (Map.Entry<String, String> entry : env.entrySet()) {
        String name = "/platform/env/" + entry.getKey();
        Content content = Content.of((entry.getValue() != null) ? entry.getValue() : "");
        layout.file(name, Owner.ROOT, content);
      }
    });
  }

  /**
   * Return the name of this archive as tagged in Docker.
   *
   * @return the ephemeral builder name
   */
  ImageReference getName() {
    return this.archive.getTag();
  }

  /**
   * Return the build owner that should be used for written content.
   *
   * @return the builder owner
   */
  Owner getBuildOwner() {
    return this.buildOwner;
  }

  /**
   * Return the builder meta-data that was used to create this ephemeral builder.
   *
   * @return the builder meta-data
   */
  BuilderMetadata getBuilderMetadata() {
    return this.builderMetadata;
  }

  /**
   * Return the contents of ephemeral builder for passing to Docker.
   *
   * @return the ephemeral builder archive
   */
  ImageArchive getArchive() {
    return this.archive;
  }

  @Override
  public String toString() {
    return this.archive.getTag().toString();
  }

}
