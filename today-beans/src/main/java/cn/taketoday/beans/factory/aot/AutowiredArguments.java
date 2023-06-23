/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.aot;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Resolved arguments to be autowired.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see BeanInstanceSupplier
 * @see AutowiredMethodArgumentsResolver
 * @since 4.0
 */
@FunctionalInterface
public interface AutowiredArguments {

  /**
   * Return the resolved argument at the specified index.
   *
   * @param <T> the type of the argument
   * @param index the argument index
   * @param requiredType the required argument type
   * @return the argument
   */
  @SuppressWarnings("unchecked")
  @Nullable
  default <T> T get(int index, Class<T> requiredType) {
    Object value = getObject(index);
    if (!ClassUtils.isAssignableValue(requiredType, value)) {
      throw new IllegalArgumentException("Argument type mismatch: expected '" +
              ClassUtils.getQualifiedName(requiredType) + "' for value [" + value + "]");
    }
    return (T) value;
  }

  /**
   * Return the resolved argument at the specified index.
   *
   * @param <T> the type of the argument
   * @param index the argument index
   * @return the argument
   */
  @SuppressWarnings("unchecked")
  @Nullable
  default <T> T get(int index) {
    return (T) getObject(index);
  }

  /**
   * Return the resolved argument at the specified index.
   *
   * @param index the argument index
   * @return the argument
   */
  @Nullable
  default Object getObject(int index) {
    return toArray()[index];
  }

  /**
   * Return the arguments as an object array.
   *
   * @return the arguments as an object array
   */
  Object[] toArray();

  /**
   * Factory method to create a new {@link AutowiredArguments} instance from
   * the given object array.
   *
   * @param arguments the arguments
   * @return a new {@link AutowiredArguments} instance
   */
  static AutowiredArguments of(Object[] arguments) {
    Assert.notNull(arguments, "'arguments' must not be null");
    return () -> arguments;
  }

}
