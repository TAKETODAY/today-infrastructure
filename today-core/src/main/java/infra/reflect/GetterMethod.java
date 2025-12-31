/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.reflect;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.util.ReflectionUtils;

/**
 * Fast invocation of bean's getter method {@link java.lang.reflect.Method Method}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-13 19:46
 */
public interface GetterMethod {

  /**
   * Get property value from the specified object
   *
   * @param obj Target object to get the property from
   * @return Property value, may be null if the property doesn't exist or is null
   * @throws ReflectionException when there's an error during reflection operation
   */
  @Nullable
  Object get(Object obj);

  /**
   * Returns the underlying {@link Method} used for reading the property value.
   *
   * @return the read method, or {@code null} if not available
   * @throws ReflectionException if there's an error accessing the method
   * @since 3.0
   */
  @Nullable
  default Method getReadMethod() {
    return null;
  }

  /**
   * Create a GetterMethod from a Java reflection field.
   * If the getter method does not exist, use reflective technology.
   *
   * @param field given Java reflection field
   * @return GetterMethod
   */
  static GetterMethod forField(final Field field) {
    final Method readMethod = ReflectionUtils.getReadMethod(field);
    if (readMethod == null) {
      return forReflective(field); // fallback to Reflective
    }
    return forMethod(readMethod);
  }

  /**
   * Use fast invocation technology {@link MethodInvoker}
   *
   * @param method Java reflection {@link Method}
   * @return GetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static GetterMethod forMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.forMethod(method);
    return forMethod(accessor);
  }

  /**
   * Use fast invocation technology {@link MethodInvoker}
   *
   * @param invoker fast MethodInvoker
   * @return GetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static GetterMethod forMethod(final MethodInvoker invoker) {
    return new MethodAccessorGetterMethod(invoker);
  }

  /**
   * Use Java reflection {@link Field} technology
   *
   * @param field Field
   * @return Reflective GetterMethod
   * @see Field#get(Object)
   * @see ReflectionUtils#getField(Field, Object)
   */
  static GetterMethod forReflective(final Field field) {
    ReflectionUtils.makeAccessible(field);
    return new ReflectiveGetterMethod(field);
  }

}
