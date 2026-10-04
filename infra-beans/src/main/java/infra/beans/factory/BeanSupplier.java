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

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.function.Supplier;

import infra.util.Assert;
import infra.util.ClassUtils;

/**
 * A {@link Supplier} that retrieves a named bean from a {@link BeanFactory}.
 * Factory methods cache singleton beans on first access by default. Non-caching
 * suppliers delegate every call to the factory, preserving prototype and custom
 * scope semantics.
 * <p>A caching supplier retains the first retrieved instance and does not track
 * subsequent bean replacement or destruction. Bean lifecycle management remains
 * the responsibility of the factory.
 * <p>Creating a supplier queries bean metadata and may initialize a
 * {@link FactoryBean} to determine its product type or singleton status.
 * <p>Serialization requires the referenced factory to support serialization.
 * Cached instances are transient and are retrieved again after deserialization.
 *
 * @param <T> the bean type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/29 21:13
 */
public class BeanSupplier<T> implements Supplier<T>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String beanName;

  private final @Nullable Class<T> beanType;

  private final BeanFactory beanFactory;

  /**
   * Create a supplier that delegates each retrieval to the factory.
   *
   * @param beanFactory the factory from which to retrieve the bean
   * @param beanName the name of the bean
   * @param beanType the required type, or {@code null} to infer it from the factory
   */
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

  /**
   * Return the name used to retrieve the bean.
   *
   * @return the bean name
   */
  public String getBeanName() {
    return beanName;
  }

  /**
   * Return the factory from which the bean is retrieved.
   *
   * @return the bean factory
   */
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  /**
   * Return the type used for bean retrieval, resolved when this supplier is created.
   * <p>An explicitly supplied type is retained unchanged. Otherwise, the type is
   * inferred from the factory, with a CGLIB-generated class replaced by its
   * user-defined class.
   *
   * @return the supplied or inferred type, or {@code null} if it could not be determined
   */
  public @Nullable Class<T> getBeanType() {
    return beanType;
  }

  /**
   * Return whether this supplier caches the first retrieved instance.
   * <p>This describes the supplier's caching policy, which may differ from the
   * bean's actual scope when explicitly configured.
   *
   * @return {@code true} if this supplier caches the instance
   */
  public boolean isSingleton() {
    return false;
  }

  /**
   * Retrieve the bean using its name and the resolved type, if available.
   * <p>Caching suppliers return the first retrieved instance on subsequent calls;
   * other suppliers perform a factory lookup on every call. Failed lookups are
   * not cached.
   *
   * @return the bean instance
   * @throws infra.beans.BeansException if the bean cannot be retrieved
   */
  @SuppressWarnings("NullAway")
  @Override
  public T get() {
    return beanFactory.getBean(beanName, beanType);
  }

  // static

  /**
   * Create a supplier, inferring the bean type and caching policy from the factory.
   * <p>The generic type is not verified against the bean name at compile time.
   * Prefer the overload accepting an explicit type when a type constraint is needed.
   *
   * @param beanFactory the factory from which to retrieve the bean
   * @param beanName the name of the bean
   * @param <E> the bean type
   * @return a supplier that caches the bean if the factory reports it as a singleton
   * @see #from(BeanFactory, Class, String)
   */
  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, String beanName) {
    return from(beanFactory, null, beanName);
  }

  /**
   * Create a supplier with a caching policy determined by
   * {@link BeanFactory#isSingleton(String)} at creation time.
   *
   * @param beanFactory the factory from which to retrieve the bean
   * @param beanType the required type, or {@code null} to infer it from the factory
   * @param beanName the name of the bean
   * @param <E> the bean type
   * @return a supplier that caches the bean if the factory reports it as a singleton
   */
  public static <E> BeanSupplier<E> from(BeanFactory beanFactory, @Nullable Class<E> beanType, String beanName) {
    boolean singleton = beanFactory.isSingleton(beanName);
    return from(beanFactory, beanType, beanName, singleton);
  }

  /**
   * Create a supplier with an explicit caching policy, independent of the bean's scope.
   * <p>Enabling caching retains the first retrieved instance even for a prototype
   * or custom-scoped bean. Disabling caching delegates every call to the factory
   * but does not prevent the factory from returning a shared instance.
   *
   * @param beanFactory the factory from which to retrieve the bean
   * @param beanType the required type, or {@code null} to infer it from the factory
   * @param beanName the name of the bean
   * @param singleton whether to cache the first retrieved instance
   * @param <E> the bean type
   * @return the bean supplier
   */
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

    private transient volatile @Nullable T instance;

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
