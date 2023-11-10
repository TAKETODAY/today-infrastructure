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

package cn.taketoday.context.properties.bind;

import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.PropertyPlaceholderHandler;

/**
 * {@link PlaceholdersResolver} to resolve placeholders from {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver {

  @Nullable
  private final Iterable<PropertySource<?>> sources;

  private final PropertyPlaceholderHandler placeholderHandler;

  public PropertySourcesPlaceholdersResolver(ConfigurableEnvironment environment) {
    this(getSources(environment), null);
  }

  public PropertySourcesPlaceholdersResolver(@Nullable Iterable<PropertySource<?>> sources) {
    this(sources, null);
  }

  public PropertySourcesPlaceholdersResolver(@Nullable Iterable<PropertySource<?>> sources,
          @Nullable PropertyPlaceholderHandler placeholderHandler) {
    this.sources = sources;
    this.placeholderHandler = placeholderHandler != null ? placeholderHandler : PropertyPlaceholderHandler.nonStrict;
  }

  @Override
  public Object resolvePlaceholders(Object value) {
    if (value instanceof String) {
      return placeholderHandler.replacePlaceholders((String) value, this::resolvePlaceholder);
    }
    return value;
  }

  @Nullable
  protected String resolvePlaceholder(String placeholder) {
    if (this.sources != null) {
      for (PropertySource<?> source : this.sources) {
        Object value = source.getProperty(placeholder);
        if (value != null) {
          return String.valueOf(value);
        }
      }
    }
    return null;
  }

  private static PropertySources getSources(ConfigurableEnvironment environment) {
    Assert.notNull(environment, "Environment is required");
    return environment.getPropertySources();
  }

}
