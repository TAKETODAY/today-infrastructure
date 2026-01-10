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

package infra.context.properties.source;

import org.jspecify.annotations.Nullable;

import infra.core.env.Environment;
import infra.core.env.PropertyResolver;
import infra.core.env.PropertySource;
import infra.origin.Origin;
import infra.origin.OriginLookup;

/**
 * {@link PropertySource} that exposes {@link ConfigurationPropertySource} instances so
 * that they can be used with a {@link PropertyResolver} or added to the
 * {@link Environment}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertySourcesPropertySource extends PropertySource<Iterable<ConfigurationPropertySource>>
        implements OriginLookup<String> {

  ConfigurationPropertySourcesPropertySource(String name, Iterable<ConfigurationPropertySource> source) {
    super(name, source);
  }

  @Override
  public boolean containsProperty(String name) {
    return findConfigurationProperty(name) != null;
  }

  @Override
  @Nullable
  public Object getProperty(String name) {
    ConfigurationProperty configurationProperty = findConfigurationProperty(name);
    return configurationProperty != null ? configurationProperty.getValue() : null;
  }

  @Nullable
  @Override
  public Origin getOrigin(String name) {
    return Origin.from(findConfigurationProperty(name));
  }

  @Nullable
  private ConfigurationProperty findConfigurationProperty(String name) {
    try {
      return findConfigurationProperty(ConfigurationPropertyName.of(name, true));
    }
    catch (Exception ex) {
      return null;
    }
  }

  @Nullable
  ConfigurationProperty findConfigurationProperty(@Nullable ConfigurationPropertyName name) {
    if (name == null) {
      return null;
    }
    for (ConfigurationPropertySource configurationPropertySource : getSource()) {
      ConfigurationProperty configurationProperty = configurationPropertySource.getConfigurationProperty(name);
      if (configurationProperty != null) {
        return configurationProperty;
      }
    }
    return null;
  }

}
