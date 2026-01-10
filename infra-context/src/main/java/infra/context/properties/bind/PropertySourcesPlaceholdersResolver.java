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
