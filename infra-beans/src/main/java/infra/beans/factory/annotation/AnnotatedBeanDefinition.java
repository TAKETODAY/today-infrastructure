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

package infra.beans.factory.annotation;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanDefinition;
import infra.core.type.AnnotationMetadata;
import infra.core.type.MethodMetadata;

/**
 * Extended {@link BeanDefinition}
 * interface that exposes {@link AnnotationMetadata}
 * about its bean class - without requiring the class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotatedGenericBeanDefinition
 * @see AnnotationMetadata
 * @since 4.0 2022/3/8 21:00
 */
public interface AnnotatedBeanDefinition extends BeanDefinition {

  /**
   * Obtain the annotation metadata (as well as basic class metadata)
   * for this bean definition's bean class.
   *
   * @return the annotation metadata object (never {@code null})
   */
  AnnotationMetadata getMetadata();

  /**
   * Obtain metadata for this bean definition's factory method, if any.
   *
   * @return the factory method metadata, or {@code null} if none
   */
  @Nullable
  MethodMetadata getFactoryMethodMetadata();

}
