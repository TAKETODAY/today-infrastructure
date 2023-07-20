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

package cn.taketoday.aot.nativex;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.aot.hint.ConditionalHint;
import cn.taketoday.aot.hint.ResourceBundleHint;
import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.aot.hint.ResourcePatternHint;
import cn.taketoday.aot.hint.ResourcePatternHints;
import cn.taketoday.lang.Nullable;

/**
 * Write a {@link ResourceHints} to the JSON output expected by the GraalVM
 * {@code native-image} compiler, typically named {@code resource-config.json}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/Resources/">Accessing Resources in Native Images</a>
 * @see <a href="https://www.graalvm.org/22.1/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
class ResourceHintsWriter {

  public static void write(BasicJsonWriter writer, ResourceHints hints) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    addIfNotEmpty(attributes, "resources", toAttributes(hints));
    handleResourceBundles(attributes, hints.resourceBundleHints());
    writer.writeObject(attributes);
  }

  private static Map<String, Object> toAttributes(ResourceHints hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    addIfNotEmpty(attributes, "includes", hint.resourcePatternHints().map(ResourcePatternHints::getIncludes)
            .flatMap(List::stream).distinct().map(ResourceHintsWriter::toAttributes).toList());
    addIfNotEmpty(attributes, "excludes", hint.resourcePatternHints().map(ResourcePatternHints::getExcludes)
            .flatMap(List::stream).distinct().map(ResourceHintsWriter::toAttributes).toList());
    return attributes;
  }

  private static void handleResourceBundles(Map<String, Object> attributes, Stream<ResourceBundleHint> ressourceBundles) {
    addIfNotEmpty(attributes, "bundles", ressourceBundles.map(ResourceHintsWriter::toAttributes).toList());
  }

  private static Map<String, Object> toAttributes(ResourceBundleHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("name", hint.getBaseName());
    return attributes;
  }

  private static Map<String, Object> toAttributes(ResourcePatternHint hint) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    handleCondition(attributes, hint);
    attributes.put("pattern", hint.toRegex().toString());
    return attributes;
  }

  private static void addIfNotEmpty(Map<String, Object> attributes, String name, @Nullable Object value) {
    if (value instanceof Collection<?> collection) {
      if (!collection.isEmpty()) {
        attributes.put(name, value);
      }
    }
    else if (value instanceof Map<?, ?> map) {
      if (!map.isEmpty()) {
        attributes.put(name, value);
      }
    }
    else if (value != null) {
      attributes.put(name, value);
    }
  }

  private static void handleCondition(Map<String, Object> attributes, ConditionalHint hint) {
    if (hint.getReachableType() != null) {
      Map<String, Object> conditionAttributes = new LinkedHashMap<>();
      conditionAttributes.put("typeReachable", hint.getReachableType());
      attributes.put("condition", conditionAttributes);
    }
  }

}
