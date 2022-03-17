/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.util.CollectionUtils;

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
  public static boolean hasMatchingName(PropertySource<?> propertySource) {
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
  public static void ifNotEmpty(Map<String, Object> source, Consumer<DefaultPropertiesPropertySource> action) {
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
  public static void addOrMerge(Map<String, Object> source, PropertySources sources) {
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

  @SuppressWarnings("unchecked")
  private static void mergeIfPossible(
          Map<String, Object> source, PropertySources sources, Map<String, Object> resultingSource) {
    PropertySource<?> existingSource = sources.get(NAME);
    if (existingSource != null) {
      Object underlyingSource = existingSource.getSource();
      if (underlyingSource instanceof Map) {
        resultingSource.putAll((Map<String, Object>) underlyingSource);
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
