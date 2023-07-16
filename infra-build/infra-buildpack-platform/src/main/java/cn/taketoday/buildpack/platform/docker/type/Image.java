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

package cn.taketoday.buildpack.platform.docker.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import cn.taketoday.buildpack.platform.json.MappedObject;

/**
 * Image details as returned from {@code Docker inspect}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class Image extends MappedObject {

  private final List<String> digests;

  private final ImageConfig config;

  private final List<LayerId> layers;

  private final String os;

  private final String created;

  Image(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.digests = getDigests(getNode().at("/RepoDigests"));
    this.config = new ImageConfig(getNode().at("/Config"));
    this.layers = extractLayers(valueAt("/RootFS/Layers", String[].class));
    this.os = valueAt("/Os", String.class);
    this.created = valueAt("/Created", String.class);
  }

  private List<String> getDigests(JsonNode node) {
    if (node.isEmpty()) {
      return Collections.emptyList();
    }
    List<String> digests = new ArrayList<>();
    node.forEach((child) -> digests.add(child.asText()));
    return Collections.unmodifiableList(digests);
  }

  private List<LayerId> extractLayers(String[] layers) {
    if (layers == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(layers).map(LayerId::of).toList();
  }

  /**
   * Return the digests of the image.
   *
   * @return the image digests
   */
  public List<String> getDigests() {
    return this.digests;
  }

  /**
   * Return image config information.
   *
   * @return the image config
   */
  public ImageConfig getConfig() {
    return this.config;
  }

  /**
   * Return the layer IDs contained in the image.
   *
   * @return the layer IDs.
   */
  public List<LayerId> getLayers() {
    return this.layers;
  }

  /**
   * Return the OS of the image.
   *
   * @return the image OS
   */
  public String getOs() {
    return (this.os != null) ? this.os : "linux";
  }

  /**
   * Return the created date of the image.
   *
   * @return the image created date
   */
  public String getCreated() {
    return this.created;
  }

  /**
   * Create a new {@link Image} instance from the specified JSON content.
   *
   * @param content the JSON content
   * @return a new {@link Image} instance
   * @throws IOException on IO error
   */
  public static Image of(InputStream content) throws IOException {
    return of(content, Image::new);
  }

}
