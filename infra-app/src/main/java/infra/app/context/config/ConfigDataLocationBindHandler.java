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
