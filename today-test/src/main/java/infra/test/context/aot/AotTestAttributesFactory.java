/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.aot;

import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.aot.AotDetector;

/**
 * Factory for {@link AotTestAttributes}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class AotTestAttributesFactory {

  @Nullable
  private static volatile Map<String, String> attributes;

  private AotTestAttributesFactory() { }

  /**
   * Get the underlying attributes map.
   * <p>If the map is not already loaded, this method loads the map from the
   * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
   * AOT execution mode} and otherwise creates a new map for storing attributes
   * during the AOT processing phase.
   */
  static Map<String, String> getAttributes() {
    Map<String, String> attrs = attributes;
    if (attrs == null) {
      synchronized(AotTestAttributesFactory.class) {
        attrs = attributes;
        if (attrs == null) {
          attrs = (AotDetector.useGeneratedArtifacts() ? loadAttributesMap() : new ConcurrentHashMap<>());
          attributes = attrs;
        }
      }
    }
    return attrs;
  }

  /**
   * Reset the factory.
   * <p>Only for internal use.
   */
  static void reset() {
    synchronized(AotTestAttributesFactory.class) {
      attributes = null;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, String> loadAttributesMap() {
    String className = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_CLASS_NAME;
    String methodName = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_METHOD_NAME;
    return GeneratedMapUtils.loadMap(className, methodName);
  }

}
