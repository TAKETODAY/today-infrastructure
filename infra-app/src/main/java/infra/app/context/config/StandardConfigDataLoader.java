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
