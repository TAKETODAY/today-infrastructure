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

package infra.aop.aspectj;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanFactory;
import infra.core.Ordered;
import infra.util.ClassUtils;

/**
 * Interface implemented to provide an instance of an AspectJ aspect.
 * Decouples from Framework's bean factory.
 *
 * <p>Extends the {@link Ordered} interface
 * to express an order value for the underlying aspect in a chain.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see BeanFactory#getBean
 * @since 4.0
 */
public interface AspectInstanceFactory extends Ordered {

  /**
   * Create an instance of this factory's aspect.
   *
   * @return the aspect instance (never {@code null})
   */
  Object getAspectInstance();

  /**
   * Expose the aspect class loader that this factory uses.
   *
   * @return the aspect class loader (or {@code null} for the bootstrap loader)
   * @see ClassUtils#getDefaultClassLoader()
   */
  @Nullable
  ClassLoader getAspectClassLoader();

}
