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

import infra.lang.Assert;

/**
 * A {@link ConfigurationPropertySource} supporting name aliases.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AliasedConfigurationPropertySource implements ConfigurationPropertySource {

  public final ConfigurationPropertySource source;

  public final ConfigurationPropertyNameAliases aliases;

  AliasedConfigurationPropertySource(ConfigurationPropertySource source, ConfigurationPropertyNameAliases aliases) {
    Assert.notNull(source, "Source is required");
    Assert.notNull(aliases, "Aliases is required");
    this.source = source;
    this.aliases = aliases;
  }

  @Override
  @Nullable
  public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
    Assert.notNull(name, "Name is required");
    ConfigurationProperty result = source.getConfigurationProperty(name);
    if (result == null) {
      ConfigurationPropertyName aliasedName = aliases.getNameForAlias(name);
      result = aliasedName != null ? source.getConfigurationProperty(aliasedName) : null;
    }
    return result;
  }

  @Override
  public ConfigurationPropertyState containsDescendantOf(ConfigurationPropertyName name) {
    Assert.notNull(name, "Name is required");
    ConfigurationPropertyState result = this.source.containsDescendantOf(name);
    if (result != ConfigurationPropertyState.ABSENT) {
      return result;
    }
    for (ConfigurationPropertyName alias : aliases.getAliases(name)) {
      ConfigurationPropertyState aliasResult = this.source.containsDescendantOf(alias);
      if (aliasResult != ConfigurationPropertyState.ABSENT) {
        return aliasResult;
      }
    }
    for (ConfigurationPropertyName from : aliases) {
      for (ConfigurationPropertyName alias : aliases.getAliases(from)) {
        if (name.isAncestorOf(alias)) {
          if (this.source.getConfigurationProperty(from) != null) {
            return ConfigurationPropertyState.PRESENT;
          }
        }
      }
    }
    return ConfigurationPropertyState.ABSENT;
  }

  @Nullable
  @Override
  public Object getUnderlyingSource() {
    return this.source.getUnderlyingSource();
  }

  protected ConfigurationPropertySource getSource() {
    return this.source;
  }

}
