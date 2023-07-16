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

import com.fasterxml.jackson.databind.JsonNode;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;
import cn.taketoday.buildpack.platform.json.MappedObject;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Buildpack metadata information.
 *
 * @author Scott Frederick
 */
final class BuildpackMetadata extends MappedObject {

  private static final String LABEL_NAME = "io.buildpacks.buildpackage.metadata";

  private final String id;

  private final String version;

  private final String homepage;

  private BuildpackMetadata(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.id = valueAt("/id", String.class);
    this.version = valueAt("/version", String.class);
    this.homepage = valueAt("/homepage", String.class);
  }

  /**
   * Return the buildpack ID.
   *
   * @return the ID
   */
  String getId() {
    return this.id;
  }

  /**
   * Return the buildpack version.
   *
   * @return the version
   */
  String getVersion() {
    return this.version;
  }

  /**
   * Return the buildpack homepage address.
   *
   * @return the homepage
   */
  String getHomepage() {
    return this.homepage;
  }

  /**
   * Factory method to extract {@link BuildpackMetadata} from an image.
   *
   * @param image the source image
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuildpackMetadata fromImage(Image image) throws IOException {
    Assert.notNull(image, "Image must not be null");
    return fromImageConfig(image.getConfig());
  }

  /**
   * Factory method to extract {@link BuildpackMetadata} from image config.
   *
   * @param imageConfig the source image config
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuildpackMetadata fromImageConfig(ImageConfig imageConfig) throws IOException {
    Assert.notNull(imageConfig, "ImageConfig must not be null");
    String json = imageConfig.getLabels().get(LABEL_NAME);
    Assert.notNull(json, () -> "No '" + LABEL_NAME + "' label found in image config labels '"
            + StringUtils.collectionToCommaDelimitedString(imageConfig.getLabels().keySet()) + "'");
    return fromJson(json);
  }

  /**
   * Factory method create {@link BuildpackMetadata} from JSON.
   *
   * @param json the source JSON
   * @return the builder metadata
   * @throws IOException on IO error
   */
  static BuildpackMetadata fromJson(String json) throws IOException {
    return fromJson(SharedObjectMapper.get().readTree(json));
  }

  /**
   * Factory method create {@link BuildpackMetadata} from JSON.
   *
   * @param node the source JSON
   * @return the builder metadata
   */
  static BuildpackMetadata fromJson(JsonNode node) {
    return new BuildpackMetadata(node);
  }

}
