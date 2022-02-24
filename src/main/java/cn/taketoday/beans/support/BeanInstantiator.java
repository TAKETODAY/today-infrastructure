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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.factory.support.BeanUtils;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * bean-instantiator: bean instantiate strategy
 *
 * @author TODAY 2020-08-13 19:31
 * @see java.lang.reflect.Constructor
 */
public abstract class BeanInstantiator {

  /**
   * get internal Constructor
   *
   * @see Constructor
   * @since 4.0
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return null;
  }

  /**
   * Invoke default {@link java.lang.reflect.Constructor}
   *
   * @return returns T
   * @throws BeanInstantiationException cannot instantiate a bean
   */
  public Object instantiate() {
    return instantiate(null);
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns T
   * @throws BeanInstantiationException cannot instantiate a bean
   */
  public final Object instantiate(@Nullable Object[] args) {
    try {
      return doInstantiate(args);
    }
    catch (BeanInstantiationException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new BeanInstantiationException(this + " cannot instantiate a bean", e);
    }
  }

  // internal new-instance impl @since 4.0
  protected abstract Object doInstantiate(@Nullable Object[] args)
          throws Throwable;

  // static

  /**
   * use BeanInstantiatorGenerator to create bytecode Constructor access
   * or fallback to java reflect Constructor cause private access
   *
   * @param constructor java reflect Constructor
   * @return BeanInstantiator to construct target T
   * @see BeanInstantiatorGenerator#create()
   */
  public static BeanInstantiator fromConstructor(Constructor<?> constructor) {
    return new BeanInstantiatorGenerator(constructor).create();
  }

  /**
   * use factory-method accessor
   *
   * @param accessor factory-method accessor
   * @param obj accessor object
   * @return MethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromMethod(MethodAccessor accessor, Object obj) {
    return new MethodAccessorBeanInstantiator(accessor, obj);
  }

  /**
   * use java reflect factory-method
   *
   * @param method java reflect method
   * @param obj accessor object
   * @return MethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromMethod(Method method, Object obj) {
    return fromMethod(MethodInvoker.fromMethod(method), obj);
  }

  /**
   * use factory-method accessor
   *
   * @param accessor factory-method accessor
   * @param obj accessor object Supplier
   * @return MethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromMethod(MethodAccessor accessor, Supplier<Object> obj) {
    return new MethodAccessorBeanInstantiator(accessor, obj);
  }

  /**
   * use java reflect method
   *
   * @param method factory-method
   * @return MethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromMethod(Method method, Supplier<Object> obj) {
    return fromMethod(MethodInvoker.fromMethod(method), obj);
  }

  /**
   * use static factory-method accessor
   *
   * @param accessor static factory-method accessor
   * @return StaticMethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromStaticMethod(MethodAccessor accessor) {
    Assert.notNull(accessor, "MethodAccessor must not be null");
    return new StaticMethodAccessorBeanInstantiator(accessor);
  }

  /**
   * use static java reflect method
   *
   * @param method static factory-method
   * @return StaticMethodAccessorBeanInstantiator to construct target T
   */
  public static BeanInstantiator fromStaticMethod(Method method) {
    return fromStaticMethod(MethodInvoker.fromMethod(method));
  }

  /**
   * Get target class's {@link BeanInstantiator}
   *
   * <p>
   * just invoke Constructor, fast invoke or use java reflect
   *
   * @param targetClass Target class
   * @return {@link BeanInstantiator}
   * @throws ConstructorNotFoundException No suitable constructor
   * @see BeanUtils#obtainConstructor(Class)
   */
  public static BeanInstantiator fromClass(final Class<?> targetClass) {
    Constructor<?> suitableConstructor = BeanUtils.obtainConstructor(targetClass);
    return fromConstructor(suitableConstructor);
  }

  /**
   * @param function function
   */
  public static FunctionInstantiator fromFunction(Function<Object[], ?> function) {
    Assert.notNull(function, "instance function must not be null");
    return new FunctionInstantiator(function);
  }

  /**
   * @param supplier bean instance supplier
   */
  public static SupplierInstantiator fromSupplier(Supplier<?> supplier) {
    Assert.notNull(supplier, "instance supplier must not be null");
    return new SupplierInstantiator(supplier);
  }

  /**
   * use default constructor
   *
   * @param target target class
   */
  public static <T> BeanInstantiator fromConstructor(final Class<T> target) {
    Assert.notNull(target, "target class must not be null");
    if (target.isArray()) {
      Class<?> componentType = target.getComponentType();
      return new ArrayInstantiator(componentType);
    }
    else if (Collection.class.isAssignableFrom(target)) {
      return new CollectionInstantiator(target);
    }
    else if (Map.class.isAssignableFrom(target)) {
      return new MapInstantiator(target);
    }

    try {
      final Constructor<T> constructor = target.getDeclaredConstructor();
      return fromConstructor(constructor);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException(
              "Target class: '" + target + "' has no default constructor", e);
    }
  }

  /**
   * @param constructor java reflect Constructor
   * @return ReflectiveConstructor
   * @see ReflectiveInstantiator
   */
  public static ConstructorAccessor fromReflective(Constructor<?> constructor) {
    Assert.notNull(constructor, "constructor must not be null");
    ReflectionUtils.makeAccessible(constructor);
    return new ReflectiveInstantiator(constructor);
  }

  /**
   * Instantiates an object, WITHOUT calling it's constructor, using internal
   * sun.reflect.ReflectionFactory - a class only available on JDK's that use Sun's 1.4 (or later)
   * Java implementation. This is the best way to instantiate an object without any side effects
   * caused by the constructor - however it is not available on every platform.
   *
   * @param target target class
   * @return SunReflectionFactoryInstantiator
   * @see SunReflectionFactoryInstantiator
   * @since 4.0
   */
  public static BeanInstantiator forSerialization(final Class<?> target) {
    return new SunReflectionFactoryInstantiator(target);
  }

}
