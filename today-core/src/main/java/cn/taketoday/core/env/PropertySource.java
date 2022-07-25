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

import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * Abstract base class representing a source of name/value property pairs. The underlying
 * {@linkplain #getSource() source object} may be of any type {@code T} that encapsulates
 * properties. Examples include {@link java.util.Properties} objects, {@link java.util.Map}
 * objects, {@code ServletContext} and {@code ServletConfig} objects (for access to init
 * parameters). Explore the {@code PropertySource} type hierarchy to see provided
 * implementations.
 *
 * <p>{@code PropertySource} objects are not typically used in isolation, but rather
 * through a {@link PropertySources} object, which aggregates property sources and in
 * conjunction with a {@link PropertyResolver} implementation that can perform
 * precedence-based searches across the set of {@code PropertySources}.
 *
 * <p>{@code PropertySource} identity is determined not based on the content of
 * encapsulated properties, but rather based on the {@link #getName() name} of the
 * {@code PropertySource} alone. This is useful for manipulating {@code PropertySource}
 * objects when in collection contexts. See operations in {@link PropertySources}
 * as well as the {@link #named(String)} and {@link #toString()} methods for details.
 *
 * @param <T> the source type
 * @author Chris Beams
 * @see PropertyResolver
 * @see PropertySourcesPropertyResolver
 * @see PropertySources
 * @since 4.0
 */
public abstract class PropertySource<T> {
  private static final Logger log = LoggerFactory.getLogger(PropertySource.class);

  protected final String name;

  protected final T source;

  /**
   * Create a new {@code PropertySource} with the given name and source object.
   *
   * @param name the associated name
   * @param source the source object
   */
  public PropertySource(String name, T source) {
    Assert.hasText(name, "Property source name must contain at least one character");
    Assert.notNull(source, "Property source must not be null");
    this.name = name;
    this.source = source;
  }

  /**
   * Create a new {@code PropertySource} with the given name and with a new
   * {@code Object} instance as the underlying source.
   * <p>Often useful in testing scenarios when creating anonymous implementations
   * that never query an actual source but rather return hard-coded values.
   */
  @SuppressWarnings("unchecked")
  public PropertySource(String name) {
    this(name, (T) new Object());
  }

  /**
   * Return the name of this {@code PropertySource}.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Return the underlying source object for this {@code PropertySource}.
   */
  public T getSource() {
    return this.source;
  }

  /**
   * Return whether this {@code PropertySource} contains the given name.
   * <p>This implementation simply checks for a {@code null} return value
   * from {@link #getProperty(String)}. Subclasses may wish to implement
   * a more efficient algorithm if possible.
   *
   * @param name the property name to find
   */
  public boolean containsProperty(String name) {
    return getProperty(name) != null;
  }

  /**
   * Return the value associated with the given name,
   * or {@code null} if not found.
   *
   * @param name the property to find
   * @see PropertyResolver#getRequiredProperty(String)
   */
  @Nullable
  public abstract Object getProperty(String name);

  /**
   * This {@code PropertySource} object is equal to the given object if:
   * <ul>
   * <li>they are the same instance
   * <li>the {@code name} properties for both objects are equal
   * </ul>
   * <p>No properties other than {@code name} are evaluated.
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof PropertySource
            && Objects.equals(getName(), ((PropertySource<?>) other).getName())));
  }

  /**
   * Return a hash code derived from the {@code name} property
   * of this {@code PropertySource} object.
   */
  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(getName());
  }

  /**
   * Produce concise output (type and name) if the current log level does not include
   * debug. If debug is enabled, produce verbose output including the hash code of the
   * PropertySource instance and every name/value property pair.
   * <p>This variable verbosity is useful as a property source such as system properties
   * or environment variables may contain an arbitrary number of property pairs,
   * potentially leading too difficult to read exception and log messages.
   *
   * @see Logger#isDebugEnabled()
   */
  @Override
  public String toString() {
    if (log.isDebugEnabled()) {
      return getClass().getSimpleName() + "@" + System.identityHashCode(this)
              + " {name='" + getName() + "', properties=" + getSource() + "}";
    }
    else {
      return getClass().getSimpleName() + " {name='" + getName() + "'}";
    }
  }

  /**
   * Return a {@code PropertySource} implementation intended for collection comparison purposes only.
   * <p>Primarily for internal use, but given a collection of {@code PropertySource} objects, may be
   * used as follows:
   * <pre class="code">
   * {@code List<PropertySource<?>> sources = new ArrayList<PropertySource<?>>();
   * sources.add(new MapPropertySource("sourceA", mapA));
   * sources.add(new MapPropertySource("sourceB", mapB));
   * assert sources.contains(PropertySource.named("sourceA"));
   * assert sources.contains(PropertySource.named("sourceB"));
   * assert !sources.contains(PropertySource.named("sourceC"));
   * }</pre>
   * The returned {@code PropertySource} will throw {@code UnsupportedOperationException}
   * if any methods other than {@code equals(Object)}, {@code hashCode()}, and {@code toString()}
   * are called.
   *
   * @param name the name of the comparison {@code PropertySource} to be created and returned.
   */
  public static PropertySource<?> named(String name) {
    return new ComparisonPropertySource(name);
  }

  /**
   * {@code PropertySource} to be used as a placeholder in cases where an actual
   * property source cannot be eagerly initialized at application context
   * creation time.  For example, a {@code ServletContext}-based property source
   * must wait until the {@code ServletContext} object is available to its enclosing
   * {@code ApplicationContext}.  In such cases, a stub should be used to hold the
   * intended default position/order of the property source, then be replaced
   * during context refresh.
   *
   * @see cn.taketoday.context.support.AbstractApplicationContext#initPropertySources(ConfigurableEnvironment)
   */
  public static class StubPropertySource extends PropertySource<Object> {

    public StubPropertySource(String name) {
      super(name, new Object());
    }

    /**
     * Always returns {@code null}.
     */
    @Override
    @Nullable
    public String getProperty(String name) {
      return null;
    }
  }

  /**
   * A {@code PropertySource} implementation intended for collection comparison
   * purposes.
   *
   * @see PropertySource#named(String)
   */
  static class ComparisonPropertySource extends StubPropertySource {

    private static final String USAGE_ERROR =
            "ComparisonPropertySource instances are for use with collection comparison only";

    public ComparisonPropertySource(String name) {
      super(name);
    }

    @Override
    public Object getSource() {
      throw new UnsupportedOperationException(USAGE_ERROR);
    }

    @Override
    public boolean containsProperty(String name) {
      throw new UnsupportedOperationException(USAGE_ERROR);
    }

    @Override
    @Nullable
    public String getProperty(String name) {
      throw new UnsupportedOperationException(USAGE_ERROR);
    }
  }

}
