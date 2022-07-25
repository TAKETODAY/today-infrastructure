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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.lang.Nullable;

/**
 * Used by the {@link cn.taketoday.beans.factory.xml.DefaultBeanDefinitionDocumentReader} to
 * locate a {@link NamespaceHandler} implementation for a particular namespace URI.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see cn.taketoday.beans.factory.xml.DefaultBeanDefinitionDocumentReader
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
