/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.type.classreading;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

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
