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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.Bindable;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.origin.Origin;

/**
 * {@link BindHandler} to set the {@link Origin} of bound {@link ConfigDataLocation}
 * objects.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class ConfigDataLocationBindHandler extends AbstractBindHandler {

  @Override
  public @Nullable Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    OriginMapper originMapper = new OriginMapper(context.getConfigurationProperty());
    if (result instanceof ConfigDataLocation location) {
      return originMapper.map(location);
    }
    if (result instanceof List<?> locations) {
      return locations.stream().map(originMapper::mapIfPossible).collect(Collectors.toCollection(ArrayList::new));
    }
    if (result instanceof ConfigDataLocation[] locations) {
      return Arrays.stream(locations).map(originMapper::mapIfPossible).toArray(ConfigDataLocation[]::new);
    }
    return result;
  }

  private record OriginMapper(@Nullable ConfigurationProperty property) {

    @Nullable
    Object mapIfPossible(@Nullable Object object) {
      return (object instanceof ConfigDataLocation location) ? map(location) : object;
    }

    @Nullable
    ConfigDataLocation map(@Nullable ConfigDataLocation location) {
      if (location == null) {
        return null;
      }
      Origin origin = Origin.from(location);
      return (origin != null) ? location : location.withOrigin(Origin.from(property()));
    }

  }

}
