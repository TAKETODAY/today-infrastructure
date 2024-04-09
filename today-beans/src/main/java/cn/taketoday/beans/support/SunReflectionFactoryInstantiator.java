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

package cn.taketoday.beans.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ReflectionUtils;

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

  @Override
  public String toString() {
    return "BeanInstantiator use serialization constructor: " + constructor;
  }

  public static <T> Constructor<T> newConstructorForSerialization(Class<T> type) {
    return newConstructorForSerialization(type, javaLangObjectConstructor);
  }

  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> newConstructorForSerialization(
          Class<T> type, Constructor<?> constructor) {
    Assert.notNull(type, "type is required");
    try {
      return (Constructor<T>) newConstructorForSerialization.invoke(
              reflectionFactory, type, constructor);
    }
    catch (Throwable e) {
      Throwable throwable = ExceptionUtils.unwrapIfNecessary(e);
      throw new IllegalStateException(
              "Serialization Constructor for '%s' created failed".formatted(type), throwable);
    }
  }

}
