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

package cn.taketoday.core.type.classreading;

import cn.taketoday.core.io.Resource;

import java.io.IOException;

/**
 * Factory interface for {@link MetadataReader} instances.
 * Allows for caching a MetadataReader per original resource.
 *
 * @author Juergen Hoeller
 * @see SimpleMetadataReaderFactory
 * @see CachingMetadataReaderFactory
 * @since 4.0
 */
public interface MetadataReaderFactory {

  /**
   * Obtain a MetadataReader for the given class name.
   *
   * @param className the class name (to be resolved to a ".class" file)
   * @return a holder for the ClassReader instance (never {@code null})
   * @throws IOException in case of I/O failure
   */
  MetadataReader getMetadataReader(String className) throws IOException;

  /**
   * Obtain a MetadataReader for the given resource.
   *
   * @param resource the resource (pointing to a ".class" file)
   * @return a holder for the ClassReader instance (never {@code null})
   * @throws IOException in case of I/O failure
   */
  MetadataReader getMetadataReader(Resource resource) throws IOException;

}
