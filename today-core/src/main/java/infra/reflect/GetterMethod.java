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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.lang.Nullable;
import infra.util.ReflectionUtils;

/**
 * Fast call bean's getter Method {@link java.lang.reflect.Method Method}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-13 19:46
 */
public interface GetterMethod {

  /**
   * Get property from <code>obj</code>
   *
   * @param obj Target object
   * @return Property
   * @throws ReflectionException reflective
   */
  @Nullable
  Object get(Object obj);

  /**
   * @throws ReflectionException reflective
   * @since 3.0
   */
  @Nullable
  default Method getReadMethod() {
    return null;
  }

  /**
   * new GetterMethod from java reflect property
   * if the setter method not exist use Reflective tech
   *
   * @param field given java reflect property
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
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param method java reflect {@link Method}
   * @return GetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static GetterMethod forMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.forMethod(method);
    return forMethod(accessor);
  }

  /**
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param invoker fast MethodInvoker
   * @return GetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static GetterMethod forMethod(final MethodInvoker invoker) {
    return new MethodAccessorGetterMethod(invoker);
  }

  /**
   * use java reflect {@link Field} tech
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
