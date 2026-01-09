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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import infra.core.env.MapPropertySource;
import infra.lang.Assert;

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
    this.delegate = new DefaultIterableConfigurationPropertySource(mapPropertySource, false, DEFAULT_MAPPERS);
    putAll(map);
  }

  /**
   * Add all entries from the specified map.
   *
   * @param map the source map
   */
  public void putAll(Map<?, ?> map) {
    Assert.notNull(map, "Map is required");
    assertNotReadOnlySystemAttributesMap(map);
    map.forEach(this::put);
  }

  /**
   * Add an individual entry.
   *
   * @param name the name
   * @param value the value
   */
  public void put(Object name, Object value) {
    Assert.notNull(name, "'name' is required");
    this.source.put(name.toString(), value);
  }

  @Override
  public Object getUnderlyingSource() {
    return this.source;
  }

  @Nullable
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
