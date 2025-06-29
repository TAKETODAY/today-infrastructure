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

package infra.core.type.classreading;

import java.io.IOException;

import infra.bytecode.ClassReader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;

/**
 * Simple implementation of the {@link MetadataReaderFactory} interface,
 * creating a new ASM {@link ClassReader} for every request.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class SimpleMetadataReaderFactory extends AbstractMetadataReaderFactory {

  /**
   * Create a new SimpleMetadataReaderFactory for the default class loader.
   */
  public SimpleMetadataReaderFactory() {
    super();
  }

  /**
   * Create a new SimpleMetadataReaderFactory for the given resource loader.
   *
   * @param resourceLoader the Infra ResourceLoader to use
   * (also determines the ClassLoader to use)
   */
  public SimpleMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
    super(resourceLoader);
  }

  /**
   * Create a new SimpleMetadataReaderFactory for the given class loader.
   *
   * @param classLoader the ClassLoader to use
   */
  public SimpleMetadataReaderFactory(@Nullable ClassLoader classLoader) {
    super(classLoader);
  }

  @Override
  public MetadataReader getMetadataReader(Resource resource) throws IOException {
    return new SimpleMetadataReader(resource, getResourceLoader().getClassLoader());
  }

}
