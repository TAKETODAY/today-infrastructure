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

package cn.taketoday.core.env;

import cn.taketoday.util.ObjectUtils;

/**
 * A {@link PropertySource} implementation capable of interrogating its
 * underlying source object to enumerate all possible property name/value
 * pairs. Exposes the {@link #getPropertyNames()} method to allow callers
 * to introspect available properties without having to access the underlying
 * source object. This also facilitates a more efficient implementation of
 * {@link #containsProperty(String)}, in that it can call {@link #getPropertyNames()}
 * and iterate through the returned array rather than attempting a call to
 * {@link #getProperty(String)} which may be more expensive. Implementations may
 * consider caching the result of {@link #getPropertyNames()} to fully exploit this
 * performance opportunity.
 *
 * <p>Most framework-provided {@code PropertySource} implementations are enumerable;
 * a counter-example would be {@code JndiPropertySource} where, due to the
 * nature of JNDI it is not possible to determine all possible property names at
 * any given time; rather it is only possible to try to access a property
 * (via {@link #getProperty(String)}) in order to evaluate whether it is present
 * or not.
 *
 * @param <T> the source type
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

  /**
   * Create a new {@code EnumerablePropertySource} with the given name and source object.
   *
   * @param name the associated name
   * @param source the source object
   */
  public EnumerablePropertySource(String name, T source) {
    super(name, source);
  }

  /**
   * Create a new {@code EnumerablePropertySource} with the given name and with a new
   * {@code Object} instance as the underlying source.
   *
   * @param name the associated name
   */
  protected EnumerablePropertySource(String name) {
    super(name);
  }

  /**
   * Return whether this {@code PropertySource} contains a property with the given name.
   * <p>This implementation checks for the presence of the given name within the
   * {@link #getPropertyNames()} array.
   *
   * @param name the name of the property to find
   */
  @Override
  public boolean containsProperty(String name) {
    return ObjectUtils.containsElement(getPropertyNames(), name);
  }

  /**
   * Return the names of all properties contained by the
   * {@linkplain #getSource() source} object (never {@code null}).
   */
  public abstract String[] getPropertyNames();

}
