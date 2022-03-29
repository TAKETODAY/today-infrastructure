/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import cn.taketoday.beans.factory.config.BeanDefinition;

/**
 * Filter that can be used to exclude beans definitions from having their
 * {@link AbstractBeanDefinition#setLazyInit(boolean) lazy-init} set by the
 * {@link LazyInitializationBeanFactoryPostProcessor}.
 * <p>
 * Primarily intended to allow downstream projects to deal with edge-cases in which it is
 * not easy to support lazy-loading (such as in DSLs that dynamically create additional
 * beans). Adding an instance of this filter to the application context can be used for
 * these edge cases.
 * <p>
 * A typical example would be something like this: <pre>
 * &#64;Bean
 * public static LazyInitializationExcludeFilter integrationLazyInitializationExcludeFilter() {
 *   return LazyInitializationExcludeFilter.forBeanTypes(IntegrationFlow.class);
 * }
 * </pre>
 * <p>
 * NOTE: Beans of this type will be instantiated very early in the spring application
 * lifecycle so they should generally be declared static and not have any dependencies.
 *
 * @author Tyler Van Gorder
 * @author Philip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:53
 */
@FunctionalInterface
public interface LazyInitializationExcludeFilter {

  /**
   * Returns {@code true} if the specified bean definition should be excluded from
   * having {@code lazy-init} automatically set.
   *
   * @param beanName the bean name
   * @param beanDefinition the bean definition
   * @param beanType the bean type
   * @return {@code true} if {@code lazy-init} should not be automatically set
   */
  boolean isExcluded(String beanName, BeanDefinition beanDefinition, Class<?> beanType);

  /**
   * Factory method that creates a filter for the given bean types.
   *
   * @param types the filtered types
   * @return a new filter instance
   */
  static LazyInitializationExcludeFilter forBeanTypes(Class<?>... types) {
    return (beanName, beanDefinition, beanType) -> {
      for (Class<?> type : types) {
        if (type.isAssignableFrom(beanType)) {
          return true;
        }
      }
      return false;
    };
  }

}
