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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A {@link ConfigurationPropertySource} backed by a {@link Map} and using standard name
 * mapping rules.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MapConfigurationPropertySource implements IterableConfigurationPropertySource {

  private static final PropertyMapper[] DEFAULT_MAPPERS = { DefaultPropertyMapper.INSTANCE };

  private final Map<String, Object> source;

  private final IterableConfigurationPropertySource delegate;

  /**
   * Create a new empty {@link MapConfigurationPropertySource} instance.
   */
  public MapConfigurationPropertySource() {
    this(Collections.emptyMap());
  }

  /**
   * Create a new {@link MapConfigurationPropertySource} instance with entries copies
   * from the specified map.
   *
   * @param map the source map
   */
  public MapConfigurationPropertySource(Map<?, ?> map) {
    this.source = new LinkedHashMap<>();
    MapPropertySource mapPropertySource = new MapPropertySource("source", this.source);
    this.delegate = new FrameworkIterableConfigurationPropertySource(mapPropertySource, DEFAULT_MAPPERS);
    putAll(map);
  }

  /**
   * Add all entries from the specified map.
   *
   * @param map the source map
   */
  public void putAll(Map<?, ?> map) {
    Assert.notNull(map, "Map must not be null");
    assertNotReadOnlySystemAttributesMap(map);
    map.forEach(this::put);
  }

  /**
   * Add an individual entry.
   *
   * @param name the name
   * @param value the value
   */
  public void put(@Nullable Object name, Object value) {
    this.source.put((name != null) ? name.toString() : null, value);
  }

  @Override
  public Object getUnderlyingSource() {
    return this.source;
  }

  @Override
  public ConfigurationProperty getConfigurationProperty(ConfigurationPropertyName name) {
    return this.delegate.getConfigurationProperty(name);
  }

  @Override
  public Iterator<ConfigurationPropertyName> iterator() {
    return this.delegate.iterator();
  }

  @Override
  public Stream<ConfigurationPropertyName> stream() {
    return this.delegate.stream();
  }

  private void assertNotReadOnlySystemAttributesMap(Map<?, ?> map) {
    try {
      map.size();
    }
    catch (UnsupportedOperationException ex) {
      throw new IllegalArgumentException("Security restricted maps are not supported", ex);
    }
  }

}
