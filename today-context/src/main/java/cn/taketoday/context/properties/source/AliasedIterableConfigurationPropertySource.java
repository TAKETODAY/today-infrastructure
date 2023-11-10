/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties.source;

import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.util.CollectionUtils;

/**
 * A {@link IterableConfigurationPropertySource} supporting name aliases.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class AliasedIterableConfigurationPropertySource extends AliasedConfigurationPropertySource
        implements IterableConfigurationPropertySource {

  AliasedIterableConfigurationPropertySource(
          IterableConfigurationPropertySource source, ConfigurationPropertyNameAliases aliases) {
    super(source, aliases);
  }

  @Override
  public Stream<ConfigurationPropertyName> stream() {
    return getSource().stream().flatMap(this::addAliases);
  }

  private Stream<ConfigurationPropertyName> addAliases(ConfigurationPropertyName name) {
    Stream<ConfigurationPropertyName> names = Stream.of(name);
    List<ConfigurationPropertyName> aliases = this.aliases.getAliases(name);
    if (CollectionUtils.isEmpty(aliases)) {
      return names;
    }
    return Stream.concat(names, aliases.stream());
  }

  @Override
  protected IterableConfigurationPropertySource getSource() {
    return (IterableConfigurationPropertySource) super.getSource();
  }

}
