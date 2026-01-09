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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import infra.app.context.config.ConfigDataEnvironmentContributor.Kind;
import infra.context.properties.bind.PlaceholdersResolver;
import infra.core.conversion.ConversionService;
import infra.core.env.PropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;
import infra.util.PropertyPlaceholderHandler;

/**
 * {@link PlaceholdersResolver} backed by one or more
 * {@link ConfigDataEnvironmentContributor} instances.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataEnvironmentContributorPlaceholdersResolver implements PlaceholdersResolver {

  private final Iterable<ConfigDataEnvironmentContributor> contributors;

  @Nullable
  private final ConfigDataActivationContext activationContext;

  private final boolean failOnResolveFromInactiveContributor;

  private final PropertyPlaceholderHandler helper;

  @Nullable
  private final ConfigDataEnvironmentContributor activeContributor;

  private final ConversionService conversionService;

  ConfigDataEnvironmentContributorPlaceholdersResolver(Iterable<ConfigDataEnvironmentContributor> contributors,
          @Nullable ConfigDataActivationContext activationContext, @Nullable ConfigDataEnvironmentContributor activeContributor,
          boolean failOnResolveFromInactiveContributor, ConversionService conversionService) {
    this.contributors = contributors;
    this.activationContext = activationContext;
    this.activeContributor = activeContributor;
    this.failOnResolveFromInactiveContributor = failOnResolveFromInactiveContributor;
    this.conversionService = conversionService;
    this.helper = PropertyPlaceholderHandler.shared(true);
  }

  @Nullable
  @Override
  public Object resolvePlaceholders(@Nullable Object value) {
    if (value instanceof String) {
      return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
    }
    return value;
  }

  @Nullable
  @SuppressWarnings("NullAway")
  private String resolvePlaceholder(String placeholder) {
    Object result = null;
    for (ConfigDataEnvironmentContributor contributor : this.contributors) {
      PropertySource<?> propertySource = contributor.propertySource;
      Object value = (propertySource != null) ? propertySource.getProperty(placeholder) : null;
      if (value != null && !isActive(contributor)) {
        if (this.failOnResolveFromInactiveContributor) {
          Origin origin = OriginLookup.getOrigin(propertySource, placeholder);
          throw new InactiveConfigDataAccessException(propertySource, contributor.resource, placeholder, origin);
        }
        value = null;
      }
      result = (result != null) ? result : value;
    }
    return result != null ? convertValueIfNecessary(result) : null;
  }

  private boolean isActive(ConfigDataEnvironmentContributor contributor) {
    if (contributor == this.activeContributor) {
      return true;
    }
    if (contributor.kind != Kind.UNBOUND_IMPORT) {
      return contributor.isActive(this.activationContext);
    }
    return contributor.withBoundProperties(this.contributors, this.activationContext)
            .isActive(this.activationContext);
  }

  @Nullable
  private String convertValueIfNecessary(Object value) {
    return value instanceof String string ? string : this.conversionService.convert(value, String.class);
  }

}
