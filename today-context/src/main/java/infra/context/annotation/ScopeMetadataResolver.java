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

package infra.context.annotation;

import infra.beans.factory.config.BeanDefinition;

/**
 * Strategy interface for resolving the scope of bean definitions.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Scope
 * @since 4.0 2021/10/26 15:56
 */
@FunctionalInterface
public interface ScopeMetadataResolver {

  /**
   * Resolve the {@link ScopeMetadata} appropriate to the supplied
   * bean {@code definition}.
   * <p>Implementations can of course use any strategy they like to
   * determine the scope metadata, but some implementations that
   * immediately to mind might be to use source level annotations
   * present on {@link BeanDefinition#getBeanClassName() the class} of the
   * supplied {@code definition}, or to use metadata present in the
   * {@link BeanDefinition#getAttributeNames()} of the supplied {@code definition}.
   *
   * @param definition the target bean definition
   * @return the relevant scope metadata; never {@code null}
   */
  ScopeMetadata resolveScopeMetadata(BeanDefinition definition);

}
