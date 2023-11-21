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
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;
import cn.taketoday.buildpack.platform.json.MappedObject;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Buildpack layers metadata information.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BuildpackLayersMetadata extends MappedObject {

  private static final String LABEL_NAME = "io.buildpacks.buildpack.layers";

  private final Buildpacks buildpacks;

  private BuildpackLayersMetadata(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.buildpacks = Buildpacks.fromJson(getNode());
  }

  /**
   * Return the metadata details of a buildpack with the given ID and version.
   *
   * @param id the buildpack ID
   * @param version the buildpack version
   * @return the buildpack details or {@code null} if a buildpack with the given ID and
   * version does not exist in the metadata
   */
  BuildpackLayerDetails getBuildpack(String id, String version) {
    return this.buildpacks.getBuildpack(id, version);
  }

  /**
   * Create a {@link BuildpackLayersMetadata} from an image.
   *
   * @param image the source image
   * @return the buildpack layers metadata
   * @throws IOException on IO error
   */
  static BuildpackLayersMetadata fromImage(Image image) throws IOException {
    Assert.notNull(image, "Image is required");
    return fromImageConfig(image.getConfig());
  }

  /**
   * Create a {@link BuildpackLayersMetadata} from image config.
   *
   * @param imageConfig the source image config
   * @return the buildpack layers metadata
   * @throws IOException on IO error
   */
  static BuildpackLayersMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
    Assert.notNull(imageConfig, "ImageConfig is required");
    String json = imageConfig.getLabels().get(LABEL_NAME);
    Assert.notNull(json, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
            + StringUtils.collectionToCommaDelimitedString(imageConfig.getLabels().keySet()) + "'");
    return fromJson(json);
  }

  /**
   * Create a {@link BuildpackLayersMetadata} from JSON.
   *
   * @param json the source JSON
   * @return the buildpack layers metadata
   * @throws IOException on IO error
   */
  static BuildpackLayersMetadata fromJson(String json) throws IOException {
    return fromJson(SharedObjectMapper.get().readTree(json));
  }

  /**
   * Create a {@link BuildpackLayersMetadata} from JSON.
   *
   * @param node the source JSON
   * @return the buildpack layers metadata
   */
  static BuildpackLayersMetadata fromJson(JsonNode node) {
    return new BuildpackLayersMetadata(node);
  }

  private static class Buildpacks {

    private final Map<String, BuildpackVersions> buildpacks = new HashMap<>();

    private BuildpackLayerDetails getBuildpack(String id, String version) {
      if (this.buildpacks.containsKey(id)) {
        return this.buildpacks.get(id).getBuildpack(version);
      }
      return null;
    }

    private void addBuildpackVersions(String id, BuildpackVersions versions) {
      this.buildpacks.put(id, versions);
    }

    private static Buildpacks fromJson(JsonNode node) {
      Buildpacks buildpacks = new Buildpacks();
      node.fields()
              .forEachRemaining((field) -> buildpacks.addBuildpackVersions(field.getKey(),
                      BuildpackVersions.fromJson(field.getValue())));
      return buildpacks;
    }

  }

  private static class BuildpackVersions {

    private final Map<String, BuildpackLayerDetails> versions = new HashMap<>();

    private BuildpackLayerDetails getBuildpack(String version) {
      return this.versions.get(version);
    }

    private void addBuildpackVersion(String version, BuildpackLayerDetails details) {
      this.versions.put(version, details);
    }

    private static BuildpackVersions fromJson(JsonNode node) {
      BuildpackVersions versions = new BuildpackVersions();
      node.fields()
              .forEachRemaining((field) -> versions.addBuildpackVersion(field.getKey(),
                      BuildpackLayerDetails.fromJson(field.getValue())));
      return versions;
    }

  }

  static final class BuildpackLayerDetails extends MappedObject {

    private final String name;

    private final String homepage;

    private final String layerDiffId;

    private BuildpackLayerDetails(JsonNode node) {
      super(node, MethodHandles.lookup());
      this.name = valueAt("/name", String.class);
      this.homepage = valueAt("/homepage", String.class);
      this.layerDiffId = valueAt("/layerDiffID", String.class);
    }

    /**
     * Return the buildpack name.
     *
     * @return the name
     */
    String getName() {
      return this.name;
    }

    /**
     * Return the buildpack homepage address.
     *
     * @return the homepage address
     */
    String getHomepage() {
      return this.homepage;
    }

    /**
     * Return the buildpack layer {@code diffID}.
     *
     * @return the layer {@code diffID}
     */
    String getLayerDiffId() {
      return this.layerDiffId;
    }

    private static BuildpackLayerDetails fromJson(JsonNode node) {
      return new BuildpackLayerDetails(node);
    }

  }

}
