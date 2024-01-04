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
package cn.taketoday.core.io;

import java.io.IOException;

import cn.taketoday.core.env.PropertySource;
import cn.taketoday.lang.Nullable;

/**
 * Strategy interface for creating resource-based {@link PropertySource} wrappers.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultPropertySourceFactory
 * @since 4.0 2021/10/28 17:34
 */
public interface PropertySourceFactory {

  /**
   * Create a {@link PropertySource} that wraps the given resource.
   * <p>Implementations will typically create {@link ResourcePropertySource}
   * instances, with {@link PropertySourceProcessor} automatically adapting
   * property source names via {@link ResourcePropertySource#withResourceName()}
   * if necessary, e.g. when combining multiple sources for the same name
   * into a {@link cn.taketoday.core.env.CompositePropertySource}.
   * Custom implementations with custom {@link PropertySource} types need
   * to make sure to expose distinct enough names, possibly deriving from
   * {@link ResourcePropertySource} where possible.
   *
   * @param name the name of the property source
   * (can be {@code null} in which case the factory implementation
   * will have to generate a name based on the given resource)
   * @param resource the resource (potentially encoded) to wrap
   * @return the new {@link PropertySource} (never {@code null})
   * @throws IOException if resource resolution failed
   */
  PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException;

}
