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

package infra.beans.factory;

/**
 * Exception thrown in case of a bean being requested despite
 * bean creation currently not being allowed (for example, during
 * the shutdown phase of a bean factory).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanCreationNotAllowedException extends BeanCreationException {
  private final String beanName;

  /**
   * Create a new BeanCreationNotAllowedException.
   *
   * @param beanName the name of the bean requested
   * @param msg the detail message
   */
  public BeanCreationNotAllowedException(String beanName, String msg) {
    super("Error creating bean with name '%s': %s".formatted(beanName, msg));
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }
}
