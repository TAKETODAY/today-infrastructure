/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.core.reflect;

import cn.taketoday.beans.NoSuchPropertyException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @author TODAY 2020/9/11 11:06
 */
public abstract class PropertyAccessor implements SetterMethod, GetterMethod {

  @Override
  public abstract Object get(Object obj);

  @Override
  public abstract void set(Object obj, Object value);

  /**
   * read-only ?
   *
   * @since 4.0
   */
  public boolean isReadOnly() {
    return false;
  }

  // static

  /**
   * PropertyAccessor
   *
   * @param field Field
   * @return PropertyAccessor
   */
  public static PropertyAccessor fromField(final Field field) {
    final Method readMethod = ReflectionUtils.getReadMethod(field);
    final boolean isReadOnly = Modifier.isFinal(field.getModifiers());
    if (isReadOnly && readMethod != null) {
      MethodInvoker invoker = MethodInvoker.fromMethod(readMethod);
      return new ReadOnlyMethodAccessorPropertyAccessor(invoker);
    }
    final Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod != null && readMethod != null) {
      return fromMethod(writeMethod, readMethod);
    }
    if (writeMethod != null) {
      final MethodInvoker accessor = MethodInvoker.fromMethod(writeMethod);
      ReflectionUtils.makeAccessible(field);
      return new PropertyAccessor() {
        @Override
        public Object get(Object obj) {
          return ReflectionUtils.getField(field, obj);
        }

        @Override
        public void set(Object obj, Object value) {
          accessor.invoke(obj, new Object[] { value });
        }

        @Override
        public Method getWriteMethod() {
          return writeMethod;
        }
      };
    }

    if (readMethod != null) {
      ReflectionUtils.makeAccessible(field);
      final MethodInvoker accessor = MethodInvoker.fromMethod(readMethod);
      return new PropertyAccessor() {
        @Override
        public Object get(Object obj) {
          return accessor.invoke(obj, null);
        }

        @Override
        public void set(Object obj, Object value) {
          ReflectionUtils.setField(field, obj, value);
        }

        @Override
        public Method getReadMethod() {
          return readMethod;
        }
      };
    }

    // readMethod == null && setMethod == null
    return fromReflective(field);
  }

  /**
   * @throws NoSuchPropertyException No property in target class
   */
  public static PropertyAccessor from(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new NoSuchPropertyException(targetClass, name);
    }
    return fromField(field);
  }

  /**
   * getter setter is exists in a bean or pojo, use fast invoke tech {@link MethodInvoker}
   *
   * @param writeMethod setter method
   * @param readMethod getter method
   * @return PropertyAccessor
   */
  public static PropertyAccessor fromMethod(Method writeMethod, Method readMethod) {
    MethodInvoker readInvoker = MethodInvoker.fromMethod(readMethod);
    MethodInvoker writeInvoker = MethodInvoker.fromMethod(writeMethod);
    return new MethodAccessorPropertyAccessor(writeInvoker, readInvoker);
  }

  /**
   * use GetterMethod and SetterMethod tech to access property
   *
   * @param writeMethod setter method
   * @param readMethod getter method
   * @return PropertyAccessor
   */
  public static PropertyAccessor fromMethod(GetterMethod readMethod, SetterMethod writeMethod) {
    Assert.notNull(readMethod, "readMethod must not be null");
    Assert.notNull(writeMethod, "writeMethod must not be null");
    return new GetterSetterPropertyAccessor(readMethod, writeMethod);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor fromReflective(Field field) {
    return fromReflective(field, null, null);
  }

  /**
   * use java reflect {@link Field} tech
   *
   * @param field Field
   * @return Reflective PropertyAccessor
   * @see Field#get(Object)
   * @see Field#set(Object, Object)
   * @see ReflectionUtils#getField(Field, Object)
   * @see ReflectionUtils#setField(Field, Object, Object)
   */
  public static PropertyAccessor fromReflective(
          Field field, @Nullable Method writeMethod, @Nullable Method readMethod) {
    ReflectionUtils.makeAccessible(field);
    if (Modifier.isFinal(field.getModifiers())) {
      return new ReflectiveReadOnlyPropertyAccessor(field, readMethod);
    }
    return new ReflectivePropertyAccessor(field, writeMethod, readMethod);
  }

}
