/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.framework.context.config;

import cn.taketoday.context.properties.bind.PlaceholdersResolver;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.Kind;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginLookup;
import cn.taketoday.util.PropertyPlaceholderHandler;

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

  @Override
  public Object resolvePlaceholders(Object value) {
    if (value instanceof String) {
      return this.helper.replacePlaceholders((String) value, this::resolvePlaceholder);
    }
    return value;
  }

  @Nullable
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
