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

/**
 * Exception to be thrown from a FactoryBean's {@code getObject()} method
 * if the bean is not fully initialized yet, for example because it is involved
 * in a circular reference.
 *
 * <p>Note: A circular reference with a FactoryBean cannot be solved by eagerly
 * caching singleton instances like with normal beans. The reason is that
 * <i>every</i> FactoryBean needs to be fully initialized before it can
 * return the created bean, while only <i>specific</i> normal beans need
 * to be initialized - that is, if a collaborating bean actually invokes
 * them on initialization instead of just storing the reference.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FactoryBean#getObject()
 * @since 4.0
 */
public class FactoryBeanNotInitializedException extends BeansException {

  /**
   * Create a new FactoryBeanNotInitializedException with the default message.
   */
  public FactoryBeanNotInitializedException() {
    super("FactoryBean is not fully initialized yet");
  }

  /**
   * Create a new FactoryBeanNotInitializedException with the given message.
   *
   * @param msg the detail message
   */
  public FactoryBeanNotInitializedException(String msg) {
    super(msg);
  }

}
