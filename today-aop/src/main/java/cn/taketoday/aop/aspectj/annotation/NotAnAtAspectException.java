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

package cn.taketoday.aop.aspectj.annotation;

import cn.taketoday.aop.framework.AopConfigException;

/**
 * Extension of AopConfigException thrown when trying to perform
 * an advisor generation operation on a class that is not an
 * AspectJ annotation-style aspect.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NotAnAtAspectException extends AopConfigException {

  private final Class<?> nonAspectClass;

  /**
   * Create a new NotAnAtAspectException for the given class.
   *
   * @param nonAspectClass the offending class
   */
  public NotAnAtAspectException(Class<?> nonAspectClass) {
    super(nonAspectClass.getName() + " is not an @AspectJ aspect");
    this.nonAspectClass = nonAspectClass;
  }

  /**
   * Returns the offending class.
   */
  public Class<?> getNonAspectClass() {
    return this.nonAspectClass;
  }

}
