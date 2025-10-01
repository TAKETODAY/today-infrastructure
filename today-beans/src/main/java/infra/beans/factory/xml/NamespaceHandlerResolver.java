/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.xml;

import org.jspecify.annotations.Nullable;

/**
 * Used by the {@link DefaultBeanDefinitionDocumentReader} to
 * locate a {@link NamespaceHandler} implementation for a particular namespace URI.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see DefaultBeanDefinitionDocumentReader
 * @since 4.0
 */
@FunctionalInterface
public interface NamespaceHandlerResolver {

  /**
   * Resolve the namespace URI and return the located {@link NamespaceHandler}
   * implementation.
   *
   * @param namespaceUri the relevant namespace URI
   * @return the located {@link NamespaceHandler} (may be {@code null})
   */
  @Nullable
  NamespaceHandler resolve(String namespaceUri);

}
