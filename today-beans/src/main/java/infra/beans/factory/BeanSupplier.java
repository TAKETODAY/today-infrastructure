/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.util.ClassUtils;

/**
 * Bean instance supplier
 * <p>
 * supports singleton or prototype
 * </p>
 *
 * @param <T> Bean type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 21:13
 */
public class BeanSupplier<T> implements Supplier<T>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String beanName;

  @Nullable
  private final Class<T> beanType;

  private final BeanFactory beanFactory;

  @SuppressWarnings({ "unchecked", "rawtypes" })
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

  @SuppressWarnings("NullAway")
  @Override
  public T get() {
    return beanFactory.getBean(beanName, beanType);
  }

  // static

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, String beanName) {
    return from(beanFactory, null, beanName);
  }

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, @Nullable Class<E> beanType, String beanName) {
    boolean singleton = beanFactory.isSingleton(beanName);
    return from(beanFactory, beanType, beanName, singleton);
  }

  public static <E> BeanSupplier<E> from(BeanFactory beanFactory,
          @Nullable Class<E> beanType, String beanName, boolean singleton) {
    if (singleton) {
      return new SingletonBeanSupplier<>(beanFactory, beanName, beanType);
    }
    return new BeanSupplier<>(beanFactory, beanName, beanType);
  }

  private static final class SingletonBeanSupplier<T> extends BeanSupplier<T> implements Supplier<T> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private transient volatile T instance;

    SingletonBeanSupplier(BeanFactory beanFactory, String beanName, @Nullable Class<T> beanType) {
      super(beanFactory, beanName, beanType);
    }

    @Override
    public T get() {
      T instance = this.instance;
      if (instance == null) {
        synchronized(this) {
          instance = this.instance;
          if (instance == null) {
            instance = super.get();
            this.instance = instance;
          }
        }
      }
      return instance;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }
}
