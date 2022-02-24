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

package cn.taketoday.core.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

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
  public static PropertyAccessor fromField(Field field) {
    Method readMethod = ReflectionUtils.getReadMethod(field);
    boolean isReadOnly = Modifier.isFinal(field.getModifiers());
    if (isReadOnly && readMethod != null) {
      MethodInvoker invoker = MethodInvoker.fromMethod(readMethod);
      return new ReadOnlyMethodAccessorPropertyAccessor(invoker);
    }
    Method writeMethod = ReflectionUtils.getWriteMethod(field);
    if (writeMethod != null && readMethod != null) {
      return fromMethod(readMethod, writeMethod);
    }
    if (writeMethod != null) {
      MethodInvoker accessor = MethodInvoker.fromMethod(writeMethod);
      ReflectionUtils.makeAccessible(field);
      return getPropertyAccessor(field, accessor, writeMethod);
    }

    if (readMethod != null) {
      ReflectionUtils.makeAccessible(field);
      MethodInvoker accessor = MethodInvoker.fromMethod(readMethod);
      return getPropertyAccessor(accessor, field, readMethod);
    }

    // readMethod == null && setMethod == null
    return fromReflective(field);
  }

  /**
   * @throws ReflectionException No property in target class
   */
  public static PropertyAccessor from(Class<?> targetClass, String name) {
    Field field = ReflectionUtils.findField(targetClass, name);
    if (field == null) {
      throw new ReflectionException("No such property: '" + name + "' in class: " + targetClass);
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
  public static PropertyAccessor fromMethod(@Nullable Method readMethod, @Nullable Method writeMethod) {
    if (readMethod != null) {
      MethodInvoker readInvoker = MethodInvoker.fromMethod(readMethod);
      if (writeMethod == null) {
        return new ReadOnlyMethodAccessorPropertyAccessor(readInvoker);
      }
      else {
        return new MethodAccessorPropertyAccessor(readInvoker, MethodInvoker.fromMethod(writeMethod));
      }
    }
    if (writeMethod != null) {
      MethodInvoker writeInvoker = MethodInvoker.fromMethod(writeMethod);
      return new WriteOnlyPropertyAccessor() {

        @Override
        public Method getWriteMethod() {
          return writeMethod;
        }

        @Override
        public void set(Object obj, Object value) {
          writeInvoker.invoke(obj, new Object[] { value });
        }
      };
    }
    throw new IllegalArgumentException("read-write cannot be null at the same time");
  }

  /**
   * use GetterMethod and SetterMethod tech to access property
   *
   * @param writeMethod setter method
   * @param readMethod getter method
   * @return PropertyAccessor
   */
  public static PropertyAccessor fromMethod(GetterMethod readMethod, @Nullable SetterMethod writeMethod) {
    Assert.notNull(readMethod, "readMethod must not be null");
    if (writeMethod != null) {
      return new GetterSetterPropertyAccessor(readMethod, writeMethod);
    }
    return new ReadOnlyGetterMethodPropertyAccessor(readMethod);
  }

  /**
   * @param field Field
   * @return PropertyAccessor
   * @throws NullPointerException field is null
   */
  public static PropertyAccessor fromField(
          Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    boolean isReadOnly = Modifier.isFinal(field.getModifiers());
    if (isReadOnly && readMethod != null) {
      MethodInvoker invoker = MethodInvoker.fromMethod(readMethod);
      return new ReadOnlyMethodAccessorPropertyAccessor(invoker);
    }
    if (writeMethod != null && readMethod != null) {
      return fromMethod(readMethod, writeMethod);
    }
    if (writeMethod != null) {
      MethodInvoker accessor = MethodInvoker.fromMethod(writeMethod);
      ReflectionUtils.makeAccessible(field);
      return getPropertyAccessor(field, accessor, writeMethod);
    }

    if (readMethod != null) {
      ReflectionUtils.makeAccessible(field);
      MethodInvoker accessor = MethodInvoker.fromMethod(readMethod);
      return getPropertyAccessor(accessor, field, readMethod);
    }

    // readMethod == null && setMethod == null
    return fromReflective(field);
  }

  private static PropertyAccessor getPropertyAccessor(Field field, MethodInvoker accessor, @NonNull Method writeMethod) {
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

  private static PropertyAccessor getPropertyAccessor(MethodInvoker accessor, Field field, @NonNull Method readMethod) {
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
          @Nullable Field field, @Nullable Method readMethod, @Nullable Method writeMethod) {
    boolean readOnly;
    if (field != null) {
      ReflectionUtils.makeAccessible(field);
      readOnly = Modifier.isFinal(field.getModifiers());
    }
    else {
      Assert.notNull(readMethod, "read-method is required");
      readOnly = writeMethod == null;
    }
    if (readOnly) {
      return new ReflectiveReadOnlyPropertyAccessor(field, readMethod);
    }
    return new ReflectivePropertyAccessor(field, readMethod, writeMethod);
  }

}
