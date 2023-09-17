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

package cn.taketoday.app.loader.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;

/**
 * Index describing the layer to which each entry in a jar belongs. Index files are simple
 * text files that should be read from top to bottom. Each file defines the layers and
 * their content. Layer names are written as quoted strings prefixed by a dash space
 * ({@code "- "}) and with a colon ({@code ":"}) suffix. Layer content is either a file or
 * directory name written as a quoted string prefixed by space space dash space
 * ({@code "  - "}). A directory name ends with {@code /}, a file name does not. When a
 * directory name is used it means that all files inside that directory are in the same
 * layer.
 * <p>
 * Index files are designed to be compatible with YAML and may be read into a list of
 * {@code Map<String, List<String>>} instances.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class LayersIndex {

  private final Iterable<Layer> layers;

  private final Node root = new Node();

  /**
   * Create a new {@link LayersIndex} backed by the given layers.
   *
   * @param layers the layers in the index
   */
  public LayersIndex(Layer... layers) {
    this(Arrays.asList(layers));
  }

  /**
   * Create a new {@link LayersIndex} backed by the given layers.
   *
   * @param layers the layers in the index
   */
  public LayersIndex(Iterable<Layer> layers) {
    this.layers = layers;
  }

  /**
   * Add an item to the index.
   *
   * @param layer the layer of the item
   * @param name the name of the item
   */
  public void add(Layer layer, String name) {
    String[] segments = name.split("/");
    Node node = this.root;
    for (int i = 0; i < segments.length; i++) {
      boolean isDirectory = i < (segments.length - 1);
      node = node.updateOrAddNode(segments[i], isDirectory, layer);
    }
  }

  /**
   * Write the layer index to an output stream.
   *
   * @param out the destination stream
   * @throws IOException on IO error
   */
  public void writeTo(OutputStream out) throws IOException {
    MultiValueMap<Layer, String> index = new LinkedMultiValueMap<>();
    this.root.buildIndex("", index);
    index.values().forEach(Collections::sort);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    for (Layer layer : this.layers) {
      List<String> names = index.get(layer);
      writer.write("- \"" + layer + "\":\n");
      if (names != null) {
        for (String name : names) {
          writer.write("  - \"" + name + "\"\n");
        }
      }
    }
    writer.flush();
  }

  /**
   * A node within the index representing a single path segment.
   */
  private static class Node {

    private final String name;

    private final Set<Layer> layers;

    private final List<Node> children = new ArrayList<>();

    Node() {
      this.name = "";
      this.layers = new HashSet<>();
    }

    Node(String name, Layer layer) {
      this.name = name;
      this.layers = new HashSet<>(Collections.singleton(layer));
    }

    Node updateOrAddNode(String segment, boolean isDirectory, Layer layer) {
      String name = segment + (isDirectory ? "/" : "");
      for (Node child : this.children) {
        if (name.equals(child.name)) {
          child.layers.add(layer);
          return child;
        }
      }
      Node child = new Node(name, layer);
      this.children.add(child);
      return child;
    }

    void buildIndex(String path, MultiValueMap<Layer, String> index) {
      String name = path + this.name;
      if (this.layers.size() == 1) {
        index.add(this.layers.iterator().next(), name);
      }
      else {
        for (Node child : this.children) {
          child.buildIndex(name, index);
        }
      }
    }

  }

}
