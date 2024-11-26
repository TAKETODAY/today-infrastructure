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

package infra.app.context.config;

import java.io.IOException;
import java.util.List;

import infra.core.env.PropertySource;
import infra.core.io.Resource;
import infra.origin.Origin;
import infra.origin.OriginTrackedResource;

import static infra.app.context.config.ConfigDataResourceNotFoundException.throwIfDoesNotExist;

/**
 * {@link ConfigDataLoader} for {@link Resource} backed locations.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardConfigDataLoader implements ConfigDataLoader<StandardConfigDataResource> {

  private static final ConfigData.PropertySourceOptions PROFILE_SPECIFIC = ConfigData.PropertySourceOptions.always(ConfigData.Option.PROFILE_SPECIFIC);

  private static final ConfigData.PropertySourceOptions NON_PROFILE_SPECIFIC = ConfigData.PropertySourceOptions.ALWAYS_NONE;

  @Override
  public ConfigData load(ConfigDataLoaderContext context, StandardConfigDataResource resource)
          throws IOException, ConfigDataNotFoundException {
    if (resource.isEmptyDirectory()) {
      return ConfigData.EMPTY;
    }
    throwIfDoesNotExist(resource, resource.getResource());
    StandardConfigDataReference reference = resource.getReference();
    Resource originTrackedResource = OriginTrackedResource.from(resource.getResource(), Origin.from(reference.configDataLocation));
    String name = String.format("Config resource '%s' via location '%s'",
            resource, reference.configDataLocation);
    List<PropertySource<?>> propertySources = reference.propertySourceLoader.load(name, originTrackedResource);
    ConfigData.PropertySourceOptions options = resource.getProfile() != null ? PROFILE_SPECIFIC : NON_PROFILE_SPECIFIC;
    return new ConfigData(propertySources, options);
  }

}
