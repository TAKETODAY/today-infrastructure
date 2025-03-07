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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.MultiValueMap;

/**
 * Maintains a mapping of {@link ConfigurationPropertyName} aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConfigurationPropertySource#withAliases(ConfigurationPropertyNameAliases)
 * @since 4.0
 */
public final class ConfigurationPropertyNameAliases implements Iterable<ConfigurationPropertyName> {

  private final MultiValueMap<ConfigurationPropertyName, ConfigurationPropertyName>
          aliases = MultiValueMap.forLinkedHashMap();

  public ConfigurationPropertyNameAliases() { }

  public ConfigurationPropertyNameAliases(String name, String... aliases) {
    addAliases(name, aliases);
  }

  public ConfigurationPropertyNameAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
    addAliases(name, aliases);
  }

  public void addAliases(String name, String... aliases) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(aliases, "Aliases is required");
    addAliases(
            ConfigurationPropertyName.of(name),
            Arrays.stream(aliases).map(ConfigurationPropertyName::of).toArray(ConfigurationPropertyName[]::new));
  }

  public void addAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
    Assert.notNull(name, "Name is required");
    Assert.notNull(aliases, "Aliases is required");
    this.aliases.addAll(name, Arrays.asList(aliases));
  }

  public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
    return this.aliases.getOrDefault(name, Collections.emptyList());
  }

  @Nullable
  public ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
    return this.aliases.entrySet()
            .stream()
            .filter((e) -> e.getValue().contains(alias))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
  }

  @Override
  public Iterator<ConfigurationPropertyName> iterator() {
    return this.aliases.keySet().iterator();
  }

}
