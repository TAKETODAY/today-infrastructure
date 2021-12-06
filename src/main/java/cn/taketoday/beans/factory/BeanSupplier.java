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

package cn.taketoday.beans.factory;

import java.util.function.Supplier;

import cn.taketoday.lang.Nullable;

/**
 * Bean instance supplier
 * <p>
 * supports singleton or prototype
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 21:13
 */
public abstract class BeanSupplier<T> implements Supplier<T> {

  protected final String beanName;

  @Nullable
  protected final Class<T> targetClass;

  protected final BeanFactory beanFactory;

  protected BeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class<T> targetClass) {
    this.beanFactory = beanFactory;
    this.targetClass = targetClass;
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  @Nullable
  public Class<T> getTargetClass() {
    return targetClass;
  }

  public abstract boolean isSingleton();

  private final static class PrototypeBeanSupplier<T>
          extends BeanSupplier<T> implements Supplier<T> {

    private PrototypeBeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class<T> targetClass) {
      super(beanFactory, beanName, targetClass);
    }

    @Override
    public boolean isSingleton() {
      return false;
    }

    @Override
    public T get() {
      return beanFactory.getBean(beanName, targetClass);
    }
  }

  private final static class SingletonBeanSupplier<T>
          extends BeanSupplier<T> implements Supplier<T> {
    private T instance;

    SingletonBeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class<T> targetClass) {
      super(beanFactory, beanName, targetClass);
    }

    @Override
    public T get() {
      if (instance == null) { // TODO DCL ?
        instance = beanFactory.getBean(beanName, targetClass);
      }
      return instance;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }

  // static

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, String beanName) {
    return from(beanFactory, null, beanName);
  }

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, Class<E> targetClass, String beanName) {
    boolean singleton = beanFactory.isSingleton(beanName);
    return from(beanFactory, targetClass, beanName, singleton);
  }

  public static <E> BeanSupplier<E> from(
          BeanFactory beanFactory, Class<E> targetClass, String beanName, boolean singleton) {
    if (singleton) {
      return new SingletonBeanSupplier<>(beanFactory, beanName, targetClass);
    }
    return new PrototypeBeanSupplier<>(beanFactory, beanName, targetClass);
  }

}
