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

package cn.taketoday.jarmode.layertools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Layers} implementation backed by a {@code layers.idx} file.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class IndexedLayers implements Layers {

  private final Map<String, List<String>> layers = new LinkedHashMap<>();

  IndexedLayers(String indexFile) {
    String[] lines = Arrays.stream(indexFile.split("\n"))
            .map((line) -> line.replace("\r", ""))
            .filter(StringUtils::hasText)
            .toArray(String[]::new);
    List<String> contents = null;
    for (String line : lines) {
      if (line.startsWith("- ")) {
        contents = new ArrayList<>();
        this.layers.put(line.substring(3, line.length() - 2), contents);
      }
      else if (line.startsWith("  - ")) {
        contents.add(line.substring(5, line.length() - 1));
      }
      else {
        throw new IllegalStateException("Layer index file is malformed");
      }
    }
    Assert.state(!this.layers.isEmpty(), "Empty layer index file loaded");
  }

  @Override
  public Iterator<String> iterator() {
    return this.layers.keySet().iterator();
  }

  @Override
  public String getLayer(ZipEntry entry) {
    return getLayer(entry.getName());
  }

  private String getLayer(String name) {
    for (Map.Entry<String, List<String>> entry : this.layers.entrySet()) {
      for (String candidate : entry.getValue()) {
        if (candidate.equals(name) || (candidate.endsWith("/") && name.startsWith(candidate))) {
          return entry.getKey();
        }
      }
    }
    throw new IllegalStateException("No layer defined in index for file '" + name + "'");
  }

  /**
   * Get an {@link IndexedLayers} instance of possible.
   *
   * @param context the context
   * @return an {@link IndexedLayers} instance or {@code null} if this not a layered
   * jar.
   */
  static IndexedLayers get(Context context) {
    try {
      try (JarFile jarFile = new JarFile(context.getArchiveFile())) {
        Manifest manifest = jarFile.getManifest();
        String location = manifest.getMainAttributes().getValue("Infra-App-Layers-Index");
        ZipEntry entry = (location != null) ? jarFile.getEntry(location) : null;
        if (entry != null) {
          String indexFile = StreamUtils.copyToString(jarFile.getInputStream(entry), StandardCharsets.UTF_8);
          return new IndexedLayers(indexFile);
        }
      }
      return null;
    }
    catch (FileNotFoundException | NoSuchFileException ex) {
      return null;
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
