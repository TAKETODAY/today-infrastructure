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
 * Fast call bean's setter Method {@link java.lang.reflect.Method Method}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-14 00:29
 */
public interface SetterMethod {

  /**
   * set property
   * <p>
   * If value is null and target property type is primitive this method will do
   * nothing.
   *
   * @param obj Target obj
   * @param value property value
   * @throws ReflectionException If this property is read only
   */
  void set(Object obj, @Nullable Object value);

  /**
   * @throws ReflectionException If this property is read only
   * @since 3.0
   */
  @Nullable
  default Method getWriteMethod() {
    return null;
  }

  // static factory

  /**
   * new SetterMethod from java reflect property
   *
   * @param field given java reflect property
   * @return SetterMethod
   */
  static SetterMethod forField(final Field field) {
    final Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod == null) {
      return forReflective(field); // fallback to Reflective
    }
    return forMethod(writeMethod);
  }

  /**
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param method java reflect {@link Method}
   * @return SetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod forMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.forMethod(method);
    return forMethod(accessor);
  }

  /**
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param invoker fast MethodInvoker
   * @return SetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod forMethod(final MethodInvoker invoker) {
    return new MethodAccessorSetterMethod(invoker);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective SetterMethod
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  static SetterMethod forReflective(final Field field) {
    ReflectionUtils.makeAccessible(field);
    return new ReflectiveSetterMethod(field);
  }

}
