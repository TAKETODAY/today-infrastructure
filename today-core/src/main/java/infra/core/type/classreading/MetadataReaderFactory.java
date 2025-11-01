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

package infra.core.type.classreading;

import java.io.IOException;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;

/**
 * Factory interface for {@link MetadataReader} instances.
 * Allows for caching a MetadataReader per original resource.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
   * @throws ClassFormatException in case of an incompatible class format
   */
  MetadataReader getMetadataReader(String className) throws IOException;

  /**
   * Obtain a MetadataReader for the given resource.
   *
   * @param resource the resource (pointing to a ".class" file)
   * @return a holder for the ClassReader instance (never {@code null})
   * @throws IOException in case of I/O failure
   * @throws ClassFormatException in case of an incompatible class format
   */
  MetadataReader getMetadataReader(Resource resource) throws IOException;

  /**
   * Return the ResourceLoader that this MetadataReaderFactory has been
   * constructed with.
   *
   * @since 5.0
   */
  ResourceLoader getResourceLoader();

  /**
   * Create a default {@link MetadataReaderFactory} implementation that's suitable
   * for the current JVM.
   *
   * @return a new factory instance
   * @since 5.0
   */
  static MetadataReaderFactory create() {
    return MetadataReaderFactoryDelegate.create();
  }

  /**
   * Create a default {@link MetadataReaderFactory} implementation that's suitable
   * for the current JVM.
   *
   * @return a new factory instance
   * @since 5.0
   */
  static MetadataReaderFactory create(@Nullable ResourceLoader resourceLoader) {
    return MetadataReaderFactoryDelegate.create(resourceLoader);
  }

  /**
   * Create a default {@link MetadataReaderFactory} implementation that's suitable
   * for the current JVM.
   *
   * @return a new factory instance
   * @since 5.0
   */
  static MetadataReaderFactory create(@Nullable ClassLoader classLoader) {
    return MetadataReaderFactoryDelegate.create(classLoader);
  }

}
