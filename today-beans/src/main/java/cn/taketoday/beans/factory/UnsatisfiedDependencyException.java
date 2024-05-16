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

package cn.taketoday.beans.factory;

import cn.taketoday.beans.BeansException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Exception thrown when a bean depends on other beans or simple properties
 * that were not specified in the bean factory definition, although
 * dependency checking was enabled.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnsatisfiedDependencyException extends BeanCreationException {

  @Nullable
  private final InjectionPoint injectionPoint;

  /**
   * Create a new UnsatisfiedDependencyException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean requested
   * @param propertyName the name of the bean property that couldn't be satisfied
   * @param msg the detail message
   */
  public UnsatisfiedDependencyException(@Nullable String resourceDescription,
          @Nullable String beanName, String propertyName, String msg) {

    super(resourceDescription, beanName,
            "Unsatisfied dependency expressed through bean property '%s'%s".formatted(propertyName, StringUtils.isNotEmpty(msg) ? ": " + msg : ""));
    this.injectionPoint = null;
  }

  /**
   * Create a new UnsatisfiedDependencyException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean requested
   * @param propertyName the name of the bean property that couldn't be satisfied
   * @param ex the bean creation exception that indicated the unsatisfied dependency
   */
  public UnsatisfiedDependencyException(@Nullable String resourceDescription,
          @Nullable String beanName, String propertyName, BeansException ex) {

    this(resourceDescription, beanName, propertyName, ex.getMessage());
    initCause(ex);
  }

  /**
   * Create a new UnsatisfiedDependencyException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean requested
   * @param injectionPoint the injection point (field or method/constructor parameter)
   * @param msg the detail message
   */
  public UnsatisfiedDependencyException(@Nullable String resourceDescription,
          @Nullable String beanName, @Nullable InjectionPoint injectionPoint, String msg) {
    super(resourceDescription, beanName, "Unsatisfied dependency expressed through %s%s"
            .formatted(injectionPoint, StringUtils.isNotEmpty(msg) ? ": " + msg : ""));
    this.injectionPoint = injectionPoint;
  }

  /**
   * Create a new UnsatisfiedDependencyException.
   *
   * @param resourceDescription description of the resource that the bean definition came from
   * @param beanName the name of the bean requested
   * @param injectionPoint the injection point (field or method/constructor parameter)
   * @param ex the bean creation exception that indicated the unsatisfied dependency
   */
  public UnsatisfiedDependencyException(@Nullable String resourceDescription,
          @Nullable String beanName, @Nullable InjectionPoint injectionPoint, BeansException ex) {
    this(resourceDescription, beanName, injectionPoint, ex.getMessage());
    initCause(ex);
  }

  /**
   * Return the injection point (field or method/constructor parameter), if known.
   */
  @Nullable
  public InjectionPoint getInjectionPoint() {
    return this.injectionPoint;
  }

}
