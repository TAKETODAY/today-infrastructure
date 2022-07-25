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

import java.io.Serializable;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Bean instance supplier
 * <p>
 * supports singleton or prototype
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 21:13
 */
public class BeanSupplier<T> implements Supplier<T>, Serializable {

  private final String beanName;

  @Nullable
  private final Class<T> beanType;

  private final BeanFactory beanFactory;

  @SuppressWarnings("unchecked")
  protected BeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class beanType) {
    Assert.notNull(beanName, "'beanName' is required");
    Assert.notNull(beanFactory, "'beanFactory' is required");
    this.beanFactory = beanFactory;
    if (beanType == null) {
      beanType = beanFactory.getType(beanName);
      if (beanType != null) {
        beanType = ClassUtils.getUserClass(beanType);
      }
    }
    this.beanType = beanType;
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  /**
   * Return the type of the contained bean.
   * <p>If the bean type is a CGLIB-generated class, the original user-defined
   * class is returned.
   */
  @Nullable
  public Class<T> getBeanType() {
    return beanType;
  }

  public boolean isSingleton() {
    return false;
  }

  @Override
  public T get() {
    return beanFactory.getBean(beanName, beanType);
  }

  // static

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, String beanName) {
    return from(beanFactory, null, beanName);
  }

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, Class<E> beanType, String beanName) {
    boolean singleton = beanFactory.isSingleton(beanName);
    return from(beanFactory, beanType, beanName, singleton);
  }

  public static <E> BeanSupplier<E> from(
          BeanFactory beanFactory, Class<E> beanType, String beanName, boolean singleton) {
    if (singleton) {
      return new SingletonBeanSupplier<>(beanFactory, beanName, beanType);
    }
    return new BeanSupplier<>(beanFactory, beanName, beanType);
  }

  private final static class SingletonBeanSupplier<T>
          extends BeanSupplier<T> implements Supplier<T> {
    private transient T instance;

    SingletonBeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class<T> beanType) {
      super(beanFactory, beanName, beanType);
    }

    @Override
    public T get() {
      T instance = this.instance;
      if (instance == null) { // TODO DCL ?
        instance = super.get();
        this.instance = instance;
      }
      return instance;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }
}
