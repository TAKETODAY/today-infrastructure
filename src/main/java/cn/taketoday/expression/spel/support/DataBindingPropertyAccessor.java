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

package cn.taketoday.expression.spel.support;

import java.lang.reflect.Method;

import cn.taketoday.expression.PropertyAccessor;

/**
 * A {@link PropertyAccessor} variant for data binding
 * purposes, using reflection to access properties for reading and possibly writing.
 *
 * <p>A property can be referenced through a public getter method (when being read)
 * or a public setter method (when being written), and also as a public field.
 *
 * <p>This accessor is explicitly designed for user-declared properties and does not
 * resolve technical properties on {@code java.lang.Object} or {@code java.lang.Class}.
 * For unrestricted resolution, choose {@link ReflectivePropertyAccessor} instead.
 *
 * @author Juergen Hoeller
 * @see #forReadOnlyAccess()
 * @see #forReadWriteAccess()
 * @see SimpleEvaluationContext
 * @see StandardEvaluationContext
 * @see ReflectivePropertyAccessor
 * @since 4.0
 */
public final class DataBindingPropertyAccessor extends ReflectivePropertyAccessor {

  /**
   * Create a new property accessor for reading and possibly also writing.
   *
   * @param allowWrite whether to also allow for write operations
   * @see #canWrite
   */
  private DataBindingPropertyAccessor(boolean allowWrite) {
    super(allowWrite);
  }

  @Override
  protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
    Class<?> clazz = method.getDeclaringClass();
    return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
  }

  /**
   * Create a new data-binding property accessor for read-only operations.
   */
  public static DataBindingPropertyAccessor forReadOnlyAccess() {
    return new DataBindingPropertyAccessor(false);
  }

  /**
   * Create a new data-binding property accessor for read-write operations.
   */
  public static DataBindingPropertyAccessor forReadWriteAccess() {
    return new DataBindingPropertyAccessor(true);
  }

}
