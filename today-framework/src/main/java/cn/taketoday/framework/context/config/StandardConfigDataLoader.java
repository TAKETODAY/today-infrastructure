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

package cn.taketoday.framework.context.config;

import java.io.IOException;
import java.util.List;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.context.config.ConfigData.Option;
import cn.taketoday.framework.context.config.ConfigData.PropertySourceOptions;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginTrackedResource;

/**
 * {@link ConfigDataLoader} for {@link Resource} backed locations.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class StandardConfigDataLoader implements ConfigDataLoader<StandardConfigDataResource> {

  private static final PropertySourceOptions PROFILE_SPECIFIC = PropertySourceOptions.always(Option.PROFILE_SPECIFIC);

  private static final PropertySourceOptions NON_PROFILE_SPECIFIC = PropertySourceOptions.ALWAYS_NONE;

  @Override
  public ConfigData load(ConfigDataLoaderContext context, StandardConfigDataResource resource)
          throws IOException, ConfigDataNotFoundException {
    if (resource.isEmptyDirectory()) {
      return ConfigData.EMPTY;
    }
    ConfigDataResourceNotFoundException.throwIfDoesNotExist(resource, resource.getResource());
    StandardConfigDataReference reference = resource.getReference();
    Resource originTrackedResource = OriginTrackedResource.from(resource.getResource(), Origin.from(reference.getConfigDataLocation()));
    String name = String.format("Config resource '%s' via location '%s'", resource,
            reference.getConfigDataLocation());
    List<PropertySource<?>> propertySources = reference.getPropertySourceLoader().load(name, originTrackedResource);
    PropertySourceOptions options = (resource.getProfile() != null) ? PROFILE_SPECIFIC : NON_PROFILE_SPECIFIC;
    return new ConfigData(propertySources, options);
  }

}
