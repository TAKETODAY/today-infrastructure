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

package infra.context.properties.source;

import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A {@link ConfigurationPropertySource} supporting a prefix.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class PrefixedConfigurationPropertySource implements ConfigurationPropertySource {

  private final ConfigurationPropertySource source;

  private final ConfigurationPropertyName prefix;

  PrefixedConfigurationPropertySource(ConfigurationPropertySource source, String prefix) {
    Assert.notNull(source, "Source is required");
    Assert.hasText(prefix, "Prefix must not be empty");
    this.source = source;
    this.prefix = ConfigurationPropertyName.of(prefix);
  }

  protected final ConfigurationPropertyName getPrefix() {
    return this.prefix;
  }

  @Override
  @Nullable
  public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
    ConfigurationProperty configurationProperty = this.source.getConfigurationProperty(getPrefixedName(name));
    if (configurationProperty == null) {
      return null;
    }
    return ConfigurationProperty.of(
            configurationProperty.getSource(),
            name,
            configurationProperty.getValue(),
            configurationProperty.getOrigin()
    );
  }

  private ConfigurationPropertyName getPrefixedName(ConfigurationPropertyName name) {
    return this.prefix.append(name);
  }

  @Override
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    return this.source.containsDescendantOf(getPrefixedName(name));
  }

  @Override
  public Object getUnderlyingSource() {
    return this.source.getUnderlyingSource();
  }

  protected ConfigurationPropertySource getSource() {
    return this.source;
  }

}
