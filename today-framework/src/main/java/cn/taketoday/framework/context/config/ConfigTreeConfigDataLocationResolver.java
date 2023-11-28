/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.context.config.LocationResourceLoader.ResourceType;

/**
 * {@link ConfigDataLocationResolver} for config tree locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConfigTreeConfigDataLocationResolver implements ConfigDataLocationResolver<ConfigTreeConfigDataResource> {

  private static final String PREFIX = "configtree:";

  private final LocationResourceLoader resourceLoader;

  public ConfigTreeConfigDataLocationResolver(ResourceLoader resourceLoader) {
    this.resourceLoader = new LocationResourceLoader(resourceLoader);
  }

  @Override
  public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
    return location.hasPrefix(PREFIX);
  }

  @Override
  public List<ConfigTreeConfigDataResource> resolve(
          ConfigDataLocationResolverContext context, ConfigDataLocation location) {
    try {
      return resolve(location.getNonPrefixedValue(PREFIX));
    }
    catch (IOException ex) {
      throw new ConfigDataLocationNotFoundException(location, ex);
    }
  }

  private List<ConfigTreeConfigDataResource> resolve(String location) throws IOException {
    if (!location.endsWith("/")) {
      throw new IllegalArgumentException(String.format("Config tree location '%s' must end with '/'", location));
    }
    if (!resourceLoader.isPattern(location)) {
      return Collections.singletonList(new ConfigTreeConfigDataResource(location));
    }
    List<Resource> resources = resourceLoader.getResources(location, ResourceType.DIRECTORY);
    var resolved = new ArrayList<ConfigTreeConfigDataResource>(resources.size());
    for (Resource resource : resources) {
      resolved.add(new ConfigTreeConfigDataResource(resource.getFile().toPath()));
    }
    return resolved;
  }

}
