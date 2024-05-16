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

/**
 * Exception thrown in case of a reference to a bean that's currently in creation.
 * Typically happens when constructor autowiring matches the currently constructed bean.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanCurrentlyInCreationException extends BeanCreationException {

  /**
   * Create a new BeanCurrentlyInCreationException,
   * with a default error message that indicates a circular reference.
   *
   * @param beanName the name of the bean requested
   */
  public BeanCurrentlyInCreationException(String beanName) {
    super(beanName, "Requested bean is currently in creation: " +
            "Is there an unresolvable circular reference or an asynchronous initialization dependency?");
  }

  /**
   * Create a new BeanCurrentlyInCreationException.
   *
   * @param beanName the name of the bean requested
   * @param msg the detail message
   */
  public BeanCurrentlyInCreationException(String beanName, String msg) {
    super(beanName, msg);
  }

}
