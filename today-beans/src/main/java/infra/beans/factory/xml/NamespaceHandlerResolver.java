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
