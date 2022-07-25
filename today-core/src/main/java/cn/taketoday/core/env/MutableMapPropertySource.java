/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.core.env;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mutable MapPropertySource
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
   * @return this {@link MockPropertySource} instance
   */
  public MutableMapPropertySource withProperty(String name, Object value) {
    this.setProperty(name, value);
    return this;
  }

}
