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

package cn.taketoday.aop.support.annotation;

import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * Bean instance supplier
 * <p>
 * supports singleton or prototype
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/29 21:13</a>
 * @since 4.0
 */
public class BeanSupplier<T> implements Supplier<T> {
  private final String beanName;
  private final boolean singleton;
  private final BeanFactory beanFactory;

  @Nullable
  private final Class<T> targetClass;

  private T instance;

  public BeanSupplier(BeanFactory beanFactory, Class<T> targetClass, BeanDefinition definition) {
    this(beanFactory, targetClass, definition.getName(), definition.isSingleton());
  }

  public BeanSupplier(BeanFactory beanFactory, Class<T> targetClass, String beanName, boolean singleton) {
    this.targetClass = targetClass;
    this.beanName = beanName;
    this.singleton = singleton;
    this.beanFactory = beanFactory;
  }

  @Override
  public T get() {
    if (singleton) {
      if (instance == null) {
        instance = beanFactory.getBean(beanName, targetClass);
      }
      return instance;
    }
    return beanFactory.getBean(beanName, targetClass);
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public String getBeanName() {
    return beanName;
  }

  public boolean isSingleton() {
    return singleton;
  }

  @Nullable
  public Class<T> getTargetClass() {
    return targetClass;
  }

  // static

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, Class<E> targetClass, String beanName) {
    boolean singleton = beanFactory.isSingleton(beanName);
    return new BeanSupplier<>(beanFactory, targetClass, beanName, singleton);
  }

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, String beanName) {
    return from(beanFactory, null, beanName);
  }

}
