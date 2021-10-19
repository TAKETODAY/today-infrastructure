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

package cn.taketoday.beans.support;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Instantiates an object, WITHOUT calling it's constructor, using internal
 * sun.reflect.ReflectionFactory - a class only available on JDK's that use Sun's 1.4 (or later)
 * Java implementation. This is the best way to instantiate an object without any side effects
 * caused by the constructor - however it is not available on every platform.
 *
 * <p>
 * objenesis SunReflectionFactoryInstantiator
 * </p>
 *
 * @author Joe Walnes
 * @author TODAY 2021/9/5 16:42
 * @since 4.0
 */
public final class SunReflectionFactoryInstantiator extends BeanInstantiator {
  private static final Object reflectionFactory;
  private static final Method newConstructorForSerialization;
  private static final Constructor<Object> javaLangObjectConstructor;

  static {
    Class<?> reflectionFactoryClass = ClassUtils.resolveClassName(
            "sun.reflect.ReflectionFactory", null);
    javaLangObjectConstructor = ReflectionUtils.getConstructor(Object.class);
    newConstructorForSerialization = ReflectionUtils.getMethod(
            reflectionFactoryClass, "newConstructorForSerialization", Class.class, Constructor.class);

    Method getReflectionFactory = ReflectionUtils.getMethod(reflectionFactoryClass, "getReflectionFactory");
    reflectionFactory = ReflectionUtils.invokeMethod(getReflectionFactory, null);
  }

  private final Constructor<?> constructor;

  SunReflectionFactoryInstantiator(Class<?> type) {
    Constructor<?> constructor = newConstructorForSerialization(type);
    ReflectionUtils.makeAccessible(constructor);
    this.constructor = constructor;
  }

  @Override
  protected Object doInstantiate(@Nullable Object[] args) throws Throwable {
    return constructor.newInstance(); // serialization
  }

  public static <T> Constructor<T> newConstructorForSerialization(Class<T> type) {
    return newConstructorForSerialization(type, javaLangObjectConstructor);
  }

  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> newConstructorForSerialization(
          Class<T> type, Constructor<?> constructor) {
    Assert.notNull(type, "type must not be null");
    try {
      return (Constructor<T>) newConstructorForSerialization.invoke(
              reflectionFactory, type, constructor);
    }
    catch (Throwable e) {
      Throwable throwable = ExceptionUtils.unwrapThrowable(e);
      throw new IllegalStateException(
              "Serialization Constructor for '" + type + "' created failed", throwable);
    }
  }

}
