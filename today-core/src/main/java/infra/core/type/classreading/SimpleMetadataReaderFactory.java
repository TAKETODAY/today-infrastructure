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

import infra.bytecode.ClassReader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;

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
