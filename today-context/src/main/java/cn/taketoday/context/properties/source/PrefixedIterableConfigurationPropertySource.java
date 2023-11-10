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

import java.util.stream.Stream;

/**
 * An iterable {@link PrefixedConfigurationPropertySource}.
 *
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class PrefixedIterableConfigurationPropertySource extends PrefixedConfigurationPropertySource
        implements IterableConfigurationPropertySource {

  PrefixedIterableConfigurationPropertySource(IterableConfigurationPropertySource source, String prefix) {
    super(source, prefix);
  }

  @Override
  public Stream<ConfigurationPropertyName> stream() {
    return getSource().stream().map(this::stripPrefix);
  }

  private ConfigurationPropertyName stripPrefix(ConfigurationPropertyName name) {
    return getPrefix().isAncestorOf(name) ? name.subName(getPrefix().getNumberOfElements()) : name;
  }

  @Override
  protected IterableConfigurationPropertySource getSource() {
    return (IterableConfigurationPropertySource) super.getSource();
  }

}
