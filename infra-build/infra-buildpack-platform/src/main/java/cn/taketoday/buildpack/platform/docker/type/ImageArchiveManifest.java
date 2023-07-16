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
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import cn.taketoday.buildpack.platform.json.MappedObject;

/**
 * Image archive manifest information.
 *
 * @author Scott Frederick
 * @since 4.0
 */
public class ImageArchiveManifest extends MappedObject {

  private final List<ManifestEntry> entries = new ArrayList<>();

  protected ImageArchiveManifest(JsonNode node) {
    super(node, MethodHandles.lookup());
    getNode().elements().forEachRemaining((element) -> this.entries.add(ManifestEntry.of(element)));
  }

  /**
   * Return the entries contained in the manifest.
   *
   * @return the manifest entries
   */
  public List<ManifestEntry> getEntries() {
    return this.entries;
  }

  /**
   * Create an {@link ImageArchiveManifest} from the provided JSON input stream.
   *
   * @param content the JSON input stream
   * @return a new {@link ImageArchiveManifest} instance
   * @throws IOException on IO error
   */
  public static ImageArchiveManifest of(InputStream content) throws IOException {
    return of(content, ImageArchiveManifest::new);
  }

  public static class ManifestEntry extends MappedObject {

    private final List<String> layers;

    protected ManifestEntry(JsonNode node) {
      super(node, MethodHandles.lookup());
      this.layers = extractLayers();
    }

    /**
     * Return the collection of layer IDs from a section of the manifest.
     *
     * @return a collection of layer IDs
     */
    public List<String> getLayers() {
      return this.layers;
    }

    static ManifestEntry of(JsonNode node) {
      return new ManifestEntry(node);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractLayers() {
      List<String> layers = valueAt("/Layers", List.class);
      if (layers == null) {
        return Collections.emptyList();
      }
      return layers;
    }

  }

}
