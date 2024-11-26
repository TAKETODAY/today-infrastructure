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

package infra.core.io;

import infra.lang.Nullable;

/**
 * A resolution strategy for protocol-specific resource handles.
 *
 * <p>Used as an SPI for {@link DefaultResourceLoader}, allowing for
 * custom protocols to be handled without subclassing the loader
 * implementation (or application context implementation).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see DefaultResourceLoader#addProtocolResolver
 * @since 4.0 2021/10/7 17:10
 */
@FunctionalInterface
public interface ProtocolResolver {

  /**
   * Resolve the given location against the given resource loader
   * if this implementation's protocol matches.
   *
   * @param location the user-specified resource location
   * @param resourceLoader the associated resource loader
   * @return a corresponding {@code Resource} handle if the given location
   * matches this resolver's protocol, or {@code null} otherwise
   */
  @Nullable
  Resource resolve(String location, ResourceLoader resourceLoader);

}

