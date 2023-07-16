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

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cn.taketoday.buildpack.platform.json.MappedObject;

/**
 * Image configuration information.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0
 */
public class ImageConfig extends MappedObject {

  private final Map<String, String> labels;

  private final Map<String, String> configEnv;

  ImageConfig(JsonNode node) {
    super(node, MethodHandles.lookup());
    this.labels = extractLabels();
    this.configEnv = parseConfigEnv();
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> extractLabels() {
    Map<String, String> labels = valueAt("/Labels", Map.class);
    if (labels == null) {
      return Collections.emptyMap();
    }
    return labels;
  }

  private Map<String, String> parseConfigEnv() {
    String[] entries = valueAt("/Env", String[].class);
    if (entries == null) {
      return Collections.emptyMap();
    }
    Map<String, String> env = new LinkedHashMap<>();
    for (String entry : entries) {
      int i = entry.indexOf('=');
      String name = (i != -1) ? entry.substring(0, i) : entry;
      String value = (i != -1) ? entry.substring(i + 1) : null;
      env.put(name, value);
    }
    return Collections.unmodifiableMap(env);
  }

  JsonNode getNodeCopy() {
    return super.getNode().deepCopy();
  }

  /**
   * Return the image labels. If the image has no labels, an empty {@code Map} is
   * returned.
   *
   * @return the image labels, never {@code null}
   */
  public Map<String, String> getLabels() {
    return this.labels;
  }

  /**
   * Return the image environment variables. If the image has no environment variables,
   * an empty {@code Map} is returned.
   *
   * @return the env, never {@code null}
   */
  public Map<String, String> getEnv() {
    return this.configEnv;
  }

  /**
   * Create an updated copy of this image config.
   *
   * @param update consumer to apply updates
   * @return an updated image config
   */
  public ImageConfig copy(Consumer<Update> update) {
    return new Update(this).run(update);

  }

  /**
   * Update class used to change data when creating a copy.
   */
  public static final class Update {

    private final ObjectNode copy;

    private Update(ImageConfig source) {
      this.copy = source.getNode().deepCopy();
    }

    private ImageConfig run(Consumer<Update> update) {
      update.accept(this);
      return new ImageConfig(this.copy);
    }

    /**
     * Update the image config with an additional label.
     *
     * @param label the label name
     * @param value the label value
     */
    public void withLabel(String label, String value) {
      JsonNode labels = this.copy.at("/Labels");
      if (labels.isMissingNode()) {
        labels = this.copy.putObject("Labels");
      }
      ((ObjectNode) labels).put(label, value);
    }

  }

}
