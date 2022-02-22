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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.source;

import cn.taketoday.core.env.AbstractPropertyResolver;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.lang.Nullable;

/**
 * Alternative {@link PropertySourcesPropertyResolver} implementation that recognizes
 * {@link ConfigurationPropertySourcesPropertySource} and saves duplicate calls to the
 * underlying sources if the name is a value {@link ConfigurationPropertyName}.
 *
 * @author Phillip Webb
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
    ConfigurationPropertySourcesPropertySource attached = getAttached();
    if (attached != null) {
      ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
      if (name != null) {
        try {
          return attached.findConfigurationProperty(name) != null;
        }
        catch (Exception ignored) { }
      }
    }
    return this.defaultResolver.containsProperty(key);
  }

  @Override
  public String getProperty(String key) {
    return getProperty(key, String.class, true);
  }

  @Override
  public <T> T getProperty(String key, Class<T> targetValueType) {
    return getProperty(key, targetValueType, true);
  }

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
    return convertValueIfNecessary(value, targetValueType);
  }

  @Nullable
  private Object findPropertyValue(String key) {
    ConfigurationPropertySourcesPropertySource attached = getAttached();
    if (attached != null) {
      ConfigurationPropertyName name = ConfigurationPropertyName.of(key, true);
      if (name != null) {
        try {
          ConfigurationProperty configurationProperty = attached.findConfigurationProperty(name);
          return (configurationProperty != null) ? configurationProperty.getValue() : null;
        }
        catch (Exception ignored) { }
      }
    }
    return this.defaultResolver.getProperty(key, Object.class, false);
  }

  @Nullable
  private ConfigurationPropertySourcesPropertySource getAttached() {
    ConfigurationPropertySourcesPropertySource attached = (ConfigurationPropertySourcesPropertySource) ConfigurationPropertySources
            .getAttached(this.propertySources);
    Iterable<ConfigurationPropertySource> attachedSource = attached != null ? attached.getSource() : null;
    if (attachedSource instanceof FrameworkConfigurationPropertySources cps && cps.isUsingSources(this.propertySources)) {
      return attached;
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

    @Override
    public <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
      return super.getProperty(key, targetValueType, resolveNestedPlaceholders);
    }

  }

}
