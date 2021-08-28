/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.util.ReflectionUtils;

/**
 * bean-constructor bean construct strategy
 *
 * @author TODAY 2020-08-13 19:31
 * @see java.lang.reflect.Constructor
 */
public abstract class BeanConstructor {

  /**
   * Invoke default {@link java.lang.reflect.Constructor}
   *
   * @return returns T
   *
   * @throws BeanInstantiationException
   *         cannot instantiate a bean
   */
  public Object newInstance() {
    return newInstance(null);
  }

  /**
   * Invoke {@link java.lang.reflect.Constructor} with given args
   *
   * @return returns T
   *
   * @throws BeanInstantiationException
   *         cannot instantiate a bean
   */
  public final Object newInstance(@Nullable Object[] args) {
    try {
      return doNewInstance(args);
    }
    catch (Throwable e) {
      throw new BeanInstantiationException("cannot instantiate a bean", e);
    }
  }

  // internal new-instance impl
  public abstract Object doNewInstance(@Nullable Object[] args);

  // static

  /**
   * use BeanConstructorGenerator to create bytecode Constructor access
   * or fallback to java reflect Constructor cause private access
   *
   * @param constructor
   *         java reflect Constructor
   *
   * @return BeanConstructor to construct target T
   *
   * @see BeanConstructorGenerator#create()
   */
  public static BeanConstructor fromConstructor(Constructor<?> constructor) {
    return new BeanConstructorGenerator(constructor).create();
  }

  /**
   * use factory-method accessor
   *
   * @param accessor
   *         factory-method accessor
   * @param obj
   *         accessor object
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromMethod(MethodAccessor accessor, Object obj) {
    return new MethodAccessorBeanConstructor(accessor, obj);
  }

  /**
   * use java reflect factory-method
   *
   * @param method
   *         java reflect method
   * @param obj
   *         accessor object
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromMethod(Method method, Object obj) {
    return fromMethod(MethodInvoker.fromMethod(method), obj);
  }

  /**
   * use factory-method accessor
   *
   * @param accessor
   *         factory-method accessor
   * @param obj
   *         accessor object Supplier
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromMethod(MethodAccessor accessor, Supplier<Object> obj) {
    return new MethodAccessorBeanConstructor(accessor, obj);
  }

  /**
   * use java reflect method
   *
   * @param method
   *         factory-method
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromMethod(Method method, Supplier<Object> obj) {
    return fromMethod(MethodInvoker.fromMethod(method), obj);
  }

  /**
   * use static factory-method accessor
   *
   * @param accessor
   *         static factory-method accessor
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromStaticMethod(MethodAccessor accessor) {
    Assert.notNull(accessor, "MethodAccessor must not be null");
    return new StaticMethodAccessorBeanConstructor(accessor);
  }

  /**
   * use static java reflect method
   *
   * @param method
   *         static factory-method
   *
   * @return BeanConstructor to construct target T
   */
  public static BeanConstructor fromStaticMethod(Method method) {
    return fromStaticMethod(MethodInvoker.fromMethod(method));
  }

  /**
   * Get target class's {@link BeanConstructor}
   *
   * <p>
   * just invoke Constructor, fast invoke or use java reflect
   *
   * @param targetClass
   *         Target class
   *
   * @return {@link BeanConstructor}
   *
   * @throws ConstructorNotFoundException
   *         No suitable constructor
   * @see BeanUtils#obtainConstructor(Class)
   */
  public static BeanConstructor fromClass(final Class<?> targetClass) {
    Constructor<?> suitableConstructor = BeanUtils.obtainConstructor(targetClass);
    return fromConstructor(suitableConstructor);
  }

  /**
   * @param function
   *         function
   */
  public static FunctionConstructor fromFunction(Function<Object[], ?> function) {
    Assert.notNull(function, "instance function must not be null");
    return new FunctionConstructor(function);
  }

  /**
   * @param supplier
   *         bean instance supplier
   */
  public static SupplierConstructor fromSupplier(Supplier<?> supplier) {
    Assert.notNull(supplier, "instance supplier must not be null");
    return new SupplierConstructor(supplier);
  }

  /**
   * use default constructor
   *
   * @param target
   *         target class
   */
  public static <T> BeanConstructor fromConstructor(final Class<T> target) {
    Assert.notNull(target, "target class must not be null");
    if (target.isArray()) {
      Class<?> componentType = target.getComponentType();
      return new ArrayConstructor(componentType);
    }
    else if (Collection.class.isAssignableFrom(target)) {
      return new CollectionConstructor(target);
    }
    else if (Map.class.isAssignableFrom(target)) {
      return new MapConstructor(target);
    }

    try {
      final Constructor<T> constructor = target.getDeclaredConstructor();
      return fromConstructor(constructor);
    }
    catch (NoSuchMethodException e) {
      throw new ReflectionException("Target class: '" + target + "‘ has no default constructor");
    }
  }

  /**
   * @param constructor
   *         java reflect Constructor
   *
   * @return ReflectiveConstructor
   *
   * @see ReflectiveConstructor
   */
  public static ConstructorAccessor fromReflective(Constructor<?> constructor) {
    Assert.notNull(constructor, "constructor must not be null");
    ReflectionUtils.makeAccessible(constructor);
    return new ReflectiveConstructor(constructor);
  }

}
