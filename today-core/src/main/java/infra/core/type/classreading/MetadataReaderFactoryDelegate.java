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

import infra.core.io.ResourceLoader;

/**
 * Internal delegate for instantiating {@link MetadataReaderFactory} implementations.
 * For JDK < 24, the {@link SimpleMetadataReaderFactory} is being used.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see MetadataReaderFactory
 * @since 5.0
 */
abstract class MetadataReaderFactoryDelegate {

  static MetadataReaderFactory create() {
    return new SimpleMetadataReaderFactory();
  }

  static MetadataReaderFactory create(@Nullable ResourceLoader resourceLoader) {
    return new SimpleMetadataReaderFactory(resourceLoader);
  }

  static MetadataReaderFactory create(@Nullable ClassLoader classLoader) {
    return new SimpleMetadataReaderFactory(classLoader);
  }
}