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

package cn.taketoday.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Fast call bean's setter Method {@link java.lang.reflect.Method Method}
 *
 * @author TODAY <br>
 * 2020-08-14 00:29
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
  void set(Object obj, Object value);

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
  static SetterMethod fromField(final Field field) {
    final Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod == null) {
      return fromReflective(field); // fallback to Reflective
    }
    return fromMethod(writeMethod);
  }

  /**
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param method java reflect {@link Method}
   * @return SetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod fromMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.forMethod(method);
    return fromMethod(accessor);
  }

  /**
   * use fast invoke tech {@link MethodInvoker}
   *
   * @param invoker fast MethodInvoker
   * @return SetterMethod
   * @see MethodInvoker#forMethod(Method)
   */
  static SetterMethod fromMethod(final MethodInvoker invoker) {
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
  static SetterMethod fromReflective(final Field field) {
    ReflectionUtils.makeAccessible(field);
    return new ReflectiveSetterMethod(field);
  }

}
