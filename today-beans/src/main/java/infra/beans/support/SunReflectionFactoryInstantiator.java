/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

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
@SuppressWarnings("NullAway")
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
  protected Object doInstantiate(@Nullable Object @Nullable [] args) throws Throwable {
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
  public static <T> Constructor<T> newConstructorForSerialization(Class<T> type, Constructor<?> constructor) {
    Assert.notNull(type, "type is required");
    return (Constructor<T>) ReflectionUtils.invokeMethod(newConstructorForSerialization, reflectionFactory, type, constructor);
  }

}
