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

package infra.context.properties.bind;

import org.jspecify.annotations.Nullable;

import infra.core.env.ConfigurableEnvironment;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.lang.Assert;
import infra.util.PlaceholderResolver;
import infra.util.PropertyPlaceholderHandler;

/**
 * {@link PlaceholdersResolver} to resolve placeholders from {@link PropertySources}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PropertySourcesPlaceholdersResolver implements PlaceholdersResolver, PlaceholderResolver {

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

  @Nullable
  @Override
  public Object resolvePlaceholders(@Nullable Object value) {
    if (value instanceof String) {
      return placeholderHandler.replacePlaceholders((String) value, this);
    }
    return value;
  }

  @Override
  @Nullable
  public String resolvePlaceholder(String placeholder) {
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
