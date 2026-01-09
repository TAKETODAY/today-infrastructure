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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.app.context.config.LocationResourceLoader.ResourceType;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

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
