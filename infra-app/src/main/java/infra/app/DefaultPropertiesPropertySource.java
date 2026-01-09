/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.util.CollectionUtils;

/**
 * {@link MapPropertySource} containing default properties contributed directly to a
 * {@code Application}. By convention, the {@link DefaultPropertiesPropertySource}
 * is always the last property source in the {@link Environment}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/21 23:11
 */
public class DefaultPropertiesPropertySource extends MapPropertySource {

  /**
   * The name of the 'default properties' property source.
   */
  public static final String NAME = "defaultProperties";

  /**
   * Create a new {@link DefaultPropertiesPropertySource} with the given {@code Map}
   * source.
   *
   * @param source the source map
   */
  public DefaultPropertiesPropertySource(Map<String, Object> source) {
    super(NAME, source);
  }

  /**
   * Return {@code true} if the given source is named 'defaultProperties'.
   *
   * @param propertySource the property source to check
   * @return {@code true} if the name matches
   */
  public static boolean hasMatchingName(@Nullable PropertySource<?> propertySource) {
    return propertySource != null && propertySource.getName().equals(NAME);
  }

  /**
   * Create a consume a new {@link DefaultPropertiesPropertySource} instance if the
   * provided source is not empty.
   *
   * @param source the {@code Map} source
   * @param action the action used to consume the
   * {@link DefaultPropertiesPropertySource}
   */
  public static void ifNotEmpty(@Nullable Map<String, Object> source,
          @Nullable Consumer<DefaultPropertiesPropertySource> action) {
    if (CollectionUtils.isNotEmpty(source) && action != null) {
      action.accept(new DefaultPropertiesPropertySource(source));
    }
  }

  /**
   * Add a new {@link DefaultPropertiesPropertySource} or merge with an existing one.
   *
   * @param source the {@code Map} source
   * @param sources the existing sources
   */
  public static void addOrMerge(@Nullable Map<String, Object> source, PropertySources sources) {
    if (CollectionUtils.isNotEmpty(source)) {
      Map<String, Object> resultingSource = new HashMap<>();
      DefaultPropertiesPropertySource propertySource = new DefaultPropertiesPropertySource(resultingSource);
      if (sources.contains(NAME)) {
        mergeIfPossible(source, sources, resultingSource);
        sources.replace(NAME, propertySource);
      }
      else {
        resultingSource.putAll(source);
        sources.addLast(propertySource);
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static void mergeIfPossible(Map<String, Object> source,
          PropertySources sources, Map<String, Object> resultingSource) {
    PropertySource<?> existingSource = sources.get(NAME);
    if (existingSource != null) {
      if (existingSource.getSource() instanceof Map underlyingSource) {
        resultingSource.putAll(underlyingSource);
      }
      resultingSource.putAll(source);
    }
  }

  /**
   * Move the 'defaultProperties' property source so that it's the last source in the
   * given {@link ConfigurableEnvironment}.
   *
   * @param environment the environment to update
   */
  public static void moveToEnd(ConfigurableEnvironment environment) {
    moveToEnd(environment.getPropertySources());
  }

  /**
   * Move the 'defaultProperties' property source so that it's the last source in the
   * given {@link PropertySources}.
   *
   * @param propertySources the property sources to update
   */
  public static void moveToEnd(PropertySources propertySources) {
    PropertySource<?> propertySource = propertySources.remove(NAME);
    if (propertySource != null) {
      propertySources.addLast(propertySource);
    }
  }

}
