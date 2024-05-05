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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.mock.env;

import java.util.Properties;

import cn.taketoday.core.env.PropertiesPropertySource;
import cn.taketoday.core.env.PropertySource;

/**
 * Simple {@link PropertySource} implementation for use in testing. Accepts
 * a user-provided {@link Properties} object, or if omitted during construction,
 * the implementation will initialize its own.
 *
 * The {@link #setProperty} and {@link #withProperty} methods are exposed for
 * convenience, for example:
 * <pre class="code">
 * {@code
 *   PropertySource<?> source = new MockPropertySource().withProperty("foo", "bar");
 * }
 * </pre>
 *
 * @author Chris Beams
 * @see MockEnvironment
 * @since 4.0
 */
public class MockPropertySource extends PropertiesPropertySource {

  /**
   * {@value} is the default name for {@link MockPropertySource} instances not
   * otherwise given an explicit name.
   *
   * @see #MockPropertySource()
   * @see #MockPropertySource(String)
   */
  public static final String MOCK_PROPERTIES_PROPERTY_SOURCE_NAME = "mockProperties";

  /**
   * Create a new {@code MockPropertySource} named {@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME}
   * that will maintain its own internal {@link Properties} instance.
   */
  public MockPropertySource() {
    this(new Properties());
  }

  /**
   * Create a new {@code MockPropertySource} with the given name that will
   * maintain its own internal {@link Properties} instance.
   *
   * @param name the {@linkplain #getName() name} of the property source
   */
  public MockPropertySource(String name) {
    this(name, new Properties());
  }

  /**
   * Create a new {@code MockPropertySource} named {@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME}
   * and backed by the given {@link Properties} object.
   *
   * @param properties the properties to use
   */
  public MockPropertySource(Properties properties) {
    this(MOCK_PROPERTIES_PROPERTY_SOURCE_NAME, properties);
  }

  /**
   * Create a new {@code MockPropertySource} with the given name and backed by the given
   * {@link Properties} object.
   *
   * @param name the {@linkplain #getName() name} of the property source
   * @param properties the properties to use
   */
  public MockPropertySource(String name, Properties properties) {
    super(name, properties);
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
  public MockPropertySource withProperty(String name, Object value) {
    this.setProperty(name, value);
    return this;
  }

}
