/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.annotation.config;

import java.util.Set;

import infra.beans.factory.Aware;
import infra.context.ApplicationContext;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.context.annotation.ImportSelector;
import infra.core.type.AnnotationMetadata;

/**
 * Interface that can be implemented by {@link ImportSelector} and
 * {@link ImportBeanDefinitionRegistrar} implementations when they can determine imports
 * early. The {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} interfaces
 * are quite flexible which can make it hard to tell exactly what bean definitions they
 * will add. This interface should be used when an implementation consistently results in
 * the same imports, given the same source.
 * <p>
 * Using {@link DeterminableImports} is particularly useful when working with Framework's
 * testing support. It allows for better generation of {@link ApplicationContext} cache
 * keys.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:32
 */
@FunctionalInterface
public interface DeterminableImports {

  /**
   * Return a set of objects that represent the imports. Objects within the returned
   * {@code Set} must implement a valid {@link Object#hashCode() hashCode} and
   * {@link Object#equals(Object) equals}.
   * <p>
   * Imports from multiple {@link DeterminableImports} instances may be combined by the
   * caller to create a complete set.
   * <p>
   * Unlike {@link ImportSelector} and {@link ImportBeanDefinitionRegistrar} any
   * {@link Aware} callbacks will not be invoked before this method is called.
   *
   * @param metadata the source meta-data
   * @return a key representing the annotations that actually drive the import
   */
  Set<Object> determineImports(AnnotationMetadata metadata);

}

