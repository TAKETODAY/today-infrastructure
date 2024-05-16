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
 * Exception thrown when a bean is not a factory, but a user tries to get
 * at the factory for the given bean name. Whether a bean is a factory is
 * determined by whether it implements the FactoryBean interface.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FactoryBean
 * @since 4.0
 */
public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

  /**
   * Create a new BeanIsNotAFactoryException.
   *
   * @param name the name of the bean requested
   * @param actualType the actual type returned, which did not match
   * the expected type
   */
  public BeanIsNotAFactoryException(String name, Class<?> actualType) {
    super(name, FactoryBean.class, actualType);
  }

}
