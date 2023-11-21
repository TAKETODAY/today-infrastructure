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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;
import cn.taketoday.buildpack.platform.json.MappedObject;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Builder metadata information.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class BuilderMetadata extends MappedObject {

  private static final String LABEL_NAME = "io.buildpacks.builder.metadata";

  private static final String[] EMPTY_MIRRORS = {};

  private final Stack stack;

  private final Lifecycle lifecycle;

  private final CreatedBy createdBy;

  private final List<BuildpackMetadata> buildpacks;

  BuilderMetadata(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.stack = valueAt("/stack", Stack.class);
    this.lifecycle = valueAt("/lifecycle", Lifecycle.class);
    this.createdBy = valueAt("/createdBy", CreatedBy.class);
    this.buildpacks = extractBuildpacks(getNode().at("/buildpacks"));
  }

  private List<BuildpackMetadata> extractBuildpacks(JsonNode node) {
    if (node.isEmpty()) {
      return Collections.emptyList();
    }
    List<BuildpackMetadata> entries = new ArrayList<>();
    node.forEach((child) -> entries.add(BuildpackMetadata.fromJson(child)));
    return entries;
  }

  /**
   * Return stack metadata.
   *
   * @return the stack metadata
   */
  Stack getStack() {
    return this.stack;
  }

  /**
   * Return lifecycle metadata.
   *
   * @return the lifecycle metadata
   */
  Lifecycle getLifecycle() {
    return this.lifecycle;
  }

  /**
   * Return information about who created the builder.
   *
   * @return the created by metadata
   */
  CreatedBy getCreatedBy() {
    return this.createdBy;
  }

  /**
   * Return the buildpacks that are bundled in the builder.
   *
   * @return the buildpacks
   */
  List<BuildpackMetadata> getBuildpacks() {
    return this.buildpacks;
  }

  /**
   * Create an updated copy of this metadata.
   *
   * @param update consumer to apply updates
   * @return an updated metadata instance
   */
  BuilderMetadata copy(Consumer<Update> update) {
    return new Update(this).run(update);
  }

  /**
   * Attach this metadata to the given update callback.
   *
   * @param update the update used to attach the metadata
   */
  void attachTo(ImageConfig.Update update) {
    try {
      String json = SharedObjectMapper.get().writeValueAsString(getNode());
      update.withLabel(LABEL_NAME, json);
    }
    catch (JsonProcessingException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Factory method to extract {@link BuilderMetadata} from an image.
   *
   * @param image the source image
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuilderMetadata fromImage(Image image) throws IOException {
    Assert.notNull(image, "Image is required");
    return fromImageConfig(image.getConfig());
  }

  /**
   * Factory method to extract {@link BuilderMetadata} from image config.
   *
   * @param imageConfig the image config
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuilderMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
    Assert.notNull(imageConfig, "ImageConfig is required");
    String json = imageConfig.getLabels().get(LABEL_NAME);
    Assert.notNull(json, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
            + StringUtils.collectionToCommaDelimitedString(imageConfig.getLabels().keySet()) + "'");
    return fromJson(json);
  }

  /**
   * Factory method create {@link BuilderMetadata} from some JSON.
   *
   * @param json the source JSON
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuilderMetadata fromJson(String json) throws IOException {
    return new BuilderMetadata(SharedObjectMapper.get().readTree(json));
  }

  /**
   * Stack metadata.
   */
  interface Stack {

    /**
     * Return run image metadata.
     *
     * @return the run image metadata
     */
    RunImage getRunImage();

    /**
     * Run image metadata.
     */
    interface RunImage {

      /**
       * Return the builder image reference.
       *
       * @return the image reference
       */
      String getImage();

      /**
       * Return stack mirrors.
       *
       * @return the stack mirrors
       */
      default String[] getMirrors() {
        return EMPTY_MIRRORS;
      }

    }

  }

  /**
   * Lifecycle metadata.
   */
  interface Lifecycle {

    /**
     * Return the lifecycle version.
     *
     * @return the lifecycle version
     */
    String getVersion();

    /**
     * Return the default API versions.
     *
     * @return the API versions
     */
    Api getApi();

    /**
     * Return the supported API versions.
     *
     * @return the API versions
     */
    Apis getApis();

    /**
     * Default API versions.
     */
    interface Api {

      /**
       * Return the default buildpack API version.
       *
       * @return the buildpack version
       */
      String getBuildpack();

      /**
       * Return the default platform API version.
       *
       * @return the platform version
       */
      String getPlatform();

    }

    /**
     * Supported API versions.
     */
    interface Apis {

      /**
       * Return the supported buildpack API versions.
       *
       * @return the buildpack versions
       */
      default String[] getBuildpack() {
        return valueAt(this, "/buildpack/supported", String[].class);
      }

      /**
       * Return the supported platform API versions.
       *
       * @return the platform versions
       */
      default String[] getPlatform() {
        return valueAt(this, "/platform/supported", String[].class);
      }

    }

  }

  /**
   * Created-by metadata.
   */
  interface CreatedBy {

    /**
     * Return the name of the creator.
     *
     * @return the creator name
     */
    String getName();

    /**
     * Return the version of the creator.
     *
     * @return the creator version
     */
    String getVersion();

  }

  /**
   * Update class used to change data when creating a copy.
   */
  static final class Update {

    private final ObjectNode copy;

    private Update(BuilderMetadata source) {
      this.copy = source.getNode().deepCopy();
    }

    private BuilderMetadata run(Consumer<Update> update) {
      update.accept(this);
      return new BuilderMetadata(this.copy);
    }

    /**
     * Update the builder meta-data with a specific created by section.
     *
     * @param name the name of the creator
     * @param version the version of the creator
     */
    void withCreatedBy(String name, String version) {
      ObjectNode createdBy = (ObjectNode) this.copy.at("/createdBy");
      if (createdBy == null) {
        createdBy = this.copy.putObject("createdBy");
      }
      createdBy.put("name", name);
      createdBy.put("version", version);
    }

  }

}
