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

import cn.taketoday.lang.Assert;

/**
 * A {@link ConfigurationPropertySource} supporting name aliases.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {

  private final ConfigurationPropertySource source;

  private final ConfigurationPropertyNameAliases aliases;

  AliasedConfigurationPropertySource(ConfigurationPropertySource source, ConfigurationPropertyNameAliases aliases) {
    Assert.notNull(source, "Source must not be null");
    Assert.notNull(aliases, "Aliases must not be null");
    this.source = source;
    this.aliases = aliases;
  }

  @Override
  public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
    Assert.notNull(name, "Name must not be null");
    ConfigurationProperty result = getSource().getConfigurationProperty(name);
    if (result == null) {
      ConfigurationPropertyName aliasedName = getAliases().getNameForAlias(name);
      result = getSource().getConfigurationProperty(aliasedName);
    }
    return result;
  }

  @Override
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    Assert.notNull(name, "Name must not be null");
    ConfigurationPropertyState result = this.source.containsDescendantOf(name);
    if (result != ConfigurationPropertyState.ABSENT) {
      return result;
    }
    for (ConfigurationPropertyName alias : getAliases().getAliases(name)) {
      ConfigurationPropertyState aliasResult = this.source.containsDescendantOf(alias);
      if (aliasResult != ConfigurationPropertyState.ABSENT) {
        return aliasResult;
      }
    }
    for (ConfigurationPropertyName from : getAliases()) {
      for (ConfigurationPropertyName alias : getAliases().getAliases(from)) {
        if (name.isAncestorOf(alias)) {
          if (this.source.getConfigurationProperty(from) != null) {
            return ConfigurationPropertyState.PRESENT;
          }
        }
      }
    }
    return ConfigurationPropertyState.ABSENT;
  }

  @Override
  public Object getUnderlyingSource() {
    return this.source.getUnderlyingSource();
  }

  protected ConfigurationPropertySource getSource() {
    return this.source;
  }

  protected ConfigurationPropertyNameAliases getAliases() {
    return this.aliases;
  }

}
