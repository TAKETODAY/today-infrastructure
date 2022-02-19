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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.util.Assert;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;

/**
 * Maintains a mapping of {@link ConfigurationPropertyName} aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see ConfigurationPropertySource#withAliases(ConfigurationPropertyNameAliases)
 * @since 4.0
 */
public final class ConfigurationPropertyNameAliases implements Iterable<ConfigurationPropertyName> {

  private final MultiValueMap<ConfigurationPropertyName, ConfigurationPropertyName> aliases = new LinkedMultiValueMap<>();

  public ConfigurationPropertyNameAliases() {
  }

  public ConfigurationPropertyNameAliases(String name, String... aliases) {
    addAliases(name, aliases);
  }

  public ConfigurationPropertyNameAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
    addAliases(name, aliases);
  }

  public void addAliases(String name, String... aliases) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(aliases, "Aliases must not be null");
    addAliases(ConfigurationPropertyName.of(name),
            Arrays.stream(aliases).map(ConfigurationPropertyName::of).toArray(ConfigurationPropertyName[]::new));
  }

  public void addAliases(ConfigurationPropertyName name, ConfigurationPropertyName... aliases) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(aliases, "Aliases must not be null");
    this.aliases.addAll(name, Arrays.asList(aliases));
  }

  public List<ConfigurationPropertyName> getAliases(ConfigurationPropertyName name) {
    return this.aliases.getOrDefault(name, Collections.emptyList());
  }

  public ConfigurationPropertyName getNameForAlias(ConfigurationPropertyName alias) {
    return this.aliases.entrySet().stream().filter((e) -> e.getValue().contains(alias)).map(Map.Entry::getKey)
            .findFirst().orElse(null);
  }

  @Override
  public Iterator<ConfigurationPropertyName> iterator() {
    return this.aliases.keySet().iterator();
  }

}
