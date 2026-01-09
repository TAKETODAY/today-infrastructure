/*
 * Copyright 2002-present the original author or authors.
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

package infra.core.env;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mutable MapPropertySource
 *
 * <p>
 * Simple {@link PropertySource} implementation for modify property. Accepts
 * a user-provided {@link Map} object, or if omitted during construction,
 * the implementation will initialize its own.
 *
 * The {@link #setProperty} and {@link #withProperty} methods are exposed for
 * convenience, for example:
 * <pre class="code">
 * {@code
 *   PropertySource<?> source = new MutableMapPropertySource().withProperty("foo", "bar");
 * }
 * </pre>
 *
 * @author Harry Yang 2021/10/11 15:20
 * @since 4.0
 */
public class MutableMapPropertySource extends MapPropertySource {

  /**
   * {@value} is the default name for {@link MutableMapPropertySource} instances not
   * otherwise given an explicit name.
   *
   * @see #MutableMapPropertySource()
   * @see #MutableMapPropertySource(String)
   */
  public static final String MUTABLE_MAP_PROPERTY_SOURCE_NAME = "mutable-map";

  /**
   * Create a new {@code MapPropertySource} with the given name and {@code Map}.
   *
   * @param name the associated name
   * @param source the Map source (without {@code null} values in order to get
   * consistent {@link #getProperty} and {@link #containsProperty} behavior)
   */
  public MutableMapPropertySource(String name, Map<String, Object> source) {
    super(name, source);
  }

  /**
   * Create a new {@code MockPropertySource} named {@value #MUTABLE_MAP_PROPERTY_SOURCE_NAME}
   * that will maintain its own internal {@link Properties} instance.
   */
  public MutableMapPropertySource() {
    this(new LinkedHashMap<>());
  }

  /**
   * Create a new {@code MockPropertySource} with the given name that will
   * maintain its own internal {@link Properties} instance.
   *
   * @param name the {@linkplain #getName() name} of the property source
   */
  public MutableMapPropertySource(String name) {
    this(name, new LinkedHashMap<>());
  }

  /**
   * Create a new {@code MockPropertySource} named {@value #MUTABLE_MAP_PROPERTY_SOURCE_NAME}
   * and backed by the given {@link Properties} object.
   *
   * @param map the map to use
   */
  public MutableMapPropertySource(Map<String, Object> map) {
    this(MUTABLE_MAP_PROPERTY_SOURCE_NAME, map);
  }

  /**
   * Set the given property on the underlying {@link Properties} object.
   */
  public void setProperty(String name, Object value) {
    this.source.put(name, value);
  }

  /**
   * Convenient synonym for {@link #setProperty} that returns the current instance.
   * Useful for method chaining and fluent-style use.
   *
   * @return this {@link MutableMapPropertySource} instance
   */
  public MutableMapPropertySource withProperty(String name, Object value) {
    this.setProperty(name, value);
    return this;
  }

}
