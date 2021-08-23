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
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.reflect.ConstructorAccessor;
import cn.taketoday.core.reflect.MethodAccessor;
import cn.taketoday.core.reflect.MethodInvoker;

/**
 * bean-constructor bean construct strategy
 *
 * @author TODAY 2020-08-13 19:31
 * @see java.lang.reflect.Constructor
 */
public abstract class BeanConstructor<T> {

  /**
   * Invoke default {@link java.lang.reflect.Constructor}
   *
   * @return returns T
   *
   * @throws BeanInstantiationException
   *         cannot instantiate a bean
   */
  public T newInstance() {
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
  public abstract T newInstance(@Nullable Object[] args);

  // static

  /**
   * use ConstructorAccessor
   *
   * @param constructor
   *         java reflect Constructor
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromConstructor(Constructor<T> constructor) {
    ConstructorAccessor accessor = ConstructorAccessor.fromConstructor(constructor);
    return new ConstructorAccessorBeanConstructor<>(accessor);
  }

  /**
   * use ConstructorAccessor
   *
   * @param accessor
   *         ConstructorAccessor
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromAccessor(ConstructorAccessor accessor) {
    Assert.notNull(accessor, "ConstructorAccessor must not be null");
    return new ConstructorAccessorBeanConstructor<>(accessor);
  }

  /**
   * use factory-method accessor
   *
   * @param accessor
   *         factory-method accessor
   * @param obj
   *         accessor object
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromMethod(MethodAccessor accessor, Object obj) {
    return new MethodAccessorBeanConstructor<>(accessor, obj);
  }

  /**
   * use java reflect factory-method
   *
   * @param method
   *         java reflect method
   * @param obj
   *         accessor object
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromMethod(Method method, Object obj) {
    return fromMethod(MethodInvoker.create(method), obj);
  }

  /**
   * use factory-method accessor
   *
   * @param accessor
   *         factory-method accessor
   * @param obj
   *         accessor object Supplier
   * @param <T>
   *         bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromMethod(MethodAccessor accessor, Supplier<Object> obj) {
    return new MethodAccessorBeanConstructor<>(accessor, obj);
  }

  /**
   * use java reflect method
   *
   * @param method
   *         factory-method
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromMethod(Method method, Supplier<Object> obj) {
    return fromMethod(MethodInvoker.create(method), obj);
  }

  /**
   * use static factory-method accessor
   *
   * @param accessor
   *         static factory-method accessor
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromStaticMethod(MethodAccessor accessor) {
    Assert.notNull(accessor, "MethodAccessor must not be null");
    return new StaticMethodAccessorBeanConstructor<>(accessor);
  }

  /**
   * use static java reflect method
   *
   * @param method
   *         static factory-method
   * @param <T>
   *         target bean type
   *
   * @return BeanConstructor to construct target T
   */
  public static <T> BeanConstructor<T> fromStaticMethod(Method method) {
    return fromStaticMethod(MethodInvoker.create(method));
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
   * @throws NoConstructorFoundException
   *         No suitable constructor
   */
  public static <T> BeanConstructor<T> fromClass(final Class<T> targetClass) {
    Constructor<T> suitableConstructor = BeanUtils.obtainConstructor(targetClass);
    return BeanConstructor.fromConstructor(suitableConstructor);
  }

  public static <T> BeanConstructor<T> fromFunction(Function<Object[], T> function) {
    Assert.notNull(function, "instance function must not be null");
    return new FunctionConstructor<>(function);
  }

  public static <T> BeanConstructor<T> fromSupplier(Supplier<T> supplier) {
    Assert.notNull(supplier, "instance supplier must not be null");
    return new SupplierConstructor<>(supplier);
  }

}
