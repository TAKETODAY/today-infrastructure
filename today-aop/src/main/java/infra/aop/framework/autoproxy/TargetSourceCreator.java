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

package infra.aop.framework.autoproxy;

import infra.aop.TargetSource;
import infra.lang.Nullable;

/**
 * Implementations can create special target sources, such as pooling target
 * sources, for particular beans. For example, they may base their choice
 * on attributes, such as a pooling attribute, on the target class.
 *
 * <p>AbstractAutoProxyCreator can support a number of TargetSourceCreators,
 * which will be applied in order.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 21:29
 * @since 3.0
 */
@FunctionalInterface
public interface TargetSourceCreator {

  /**
   * Create a special TargetSource for the given bean, if any.
   *
   * @param beanClass the class of the bean to create a TargetSource for
   * @param beanName the name of the bean
   * @return a special TargetSource or {@code null} if this TargetSourceCreator isn't
   * interested in the particular bean
   */
  @Nullable
  TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
