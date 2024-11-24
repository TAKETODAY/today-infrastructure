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

package infra.aop.aspectj;

import infra.beans.factory.BeanFactory;
import infra.core.Ordered;
import infra.lang.Nullable;
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
