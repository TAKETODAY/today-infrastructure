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

import infra.core.conversion.ConversionFailedException;
import infra.core.env.AbstractPropertyResolver;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.env.PropertySourcesPropertyResolver;

/**
 * Alternative {@link PropertySourcesPropertyResolver} implementation that recognizes
 * {@link ConfigurationPropertySourcesPropertySource} and saves duplicate calls to the
 * underlying sources if the name is a value {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigurationPropertySourcesPropertyResolver extends AbstractPropertyResolver {

  private final PropertySources propertySources;

  private final DefaultResolver defaultResolver;

  ConfigurationPropertySourcesPropertyResolver(PropertySources propertySources) {
    this.propertySources = propertySources;
    this.defaultResolver = new DefaultResolver(propertySources);
  }

  @Override
  public boolean containsProperty(String key) {
    var attached = getAttached();
    if (attached != null) {
      ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
      if (name != null) {
        try {
          return attached.findConfigurationProperty(name) != null;
        }
        catch (Exception ignored) {
        }
      }
    }
    return this.defaultResolver.containsProperty(key);
  }

  @Nullable
  @Override
  public String getProperty(String key) {
    return getProperty(key, String.class, true);
  }

  @Nullable
  @Override
  public <T> T getProperty(String key, Class<T> targetValueType) {
    return getProperty(key, targetValueType, true);
  }

  @Nullable
  @Override
  protected String getPropertyAsRawString(String key) {
    return getProperty(key, String.class, false);
  }

  @Nullable
  private <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
    Object value = findPropertyValue(key);
    if (value == null) {
      return null;
    }
    if (resolveNestedPlaceholders && value instanceof String) {
      value = resolveNestedPlaceholders((String) value);
    }
    try {
      return convertValueIfNecessary(value, targetValueType);
    }
    catch (ConversionFailedException ex) {
      Exception wrappedCause = new InvalidConfigurationPropertyValueException(key, value,
              "Failed to convert to type " + ex.getTargetType(), ex.getCause());
      throw new ConversionFailedException(ex.getSourceType(), ex.getTargetType(), ex.getValue(), wrappedCause);
    }
  }

  @Nullable
  private Object findPropertyValue(String key) {
    var attached = getAttached();
    if (attached != null) {
      ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
      if (name != null) {
        try {
          ConfigurationProperty configurationProperty = attached.findConfigurationProperty(name);
          return configurationProperty != null ? configurationProperty.getValue() : null;
        }
        catch (Exception ignored) {
        }
      }
    }
    return this.defaultResolver.getProperty(key, Object.class, false);
  }

  @Nullable
  private ConfigurationPropertySourcesPropertySource getAttached() {
    PropertySource<?> attached = ConfigurationPropertySources.getAttached(this.propertySources);
    Object attachedSource = attached != null ? attached.getSource() : null;
    if (attachedSource instanceof DefaultConfigurationPropertySources cps && cps.isUsingSources(this.propertySources)) {
      return (ConfigurationPropertySourcesPropertySource) attached;
    }
    return null;
  }

  /**
   * Default {@link PropertySourcesPropertyResolver} used if
   * {@link ConfigurationPropertySources} is not attached.
   */
  static class DefaultResolver extends PropertySourcesPropertyResolver {

    DefaultResolver(PropertySources propertySources) {
      super(propertySources);
    }

  }

}
